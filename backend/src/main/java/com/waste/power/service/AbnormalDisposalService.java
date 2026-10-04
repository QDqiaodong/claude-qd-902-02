package com.waste.power.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waste.power.common.BizException;
import com.waste.power.common.Dict;
import com.waste.power.domain.AbnormalDisposal;
import com.waste.power.domain.AbnormalDisposalAlarm;
import com.waste.power.domain.AbnormalDisposalTrace;
import com.waste.power.domain.EnvAlarm;
import com.waste.power.domain.Incinerator;
import com.waste.power.dto.DisposalPayload;
import com.waste.power.repository.AbnormalDisposalAlarmRepository;
import com.waste.power.repository.AbnormalDisposalRepository;
import com.waste.power.repository.AbnormalDisposalTraceRepository;
import com.waste.power.repository.EnvAlarmRepository;
import com.waste.power.repository.IncineratorRepository;
import com.waste.power.repository.StoragePitRepository;

/**
 * 异常工况处置单闭环：
 *
 * 触发（炉温/停留不达标 或 同炉严重排放告警，自动/人工均可）
 *   → 保存炉况、严重告警、供料抓斗快照
 *   → 冻结该炉后续投料（不改动任何已完成运行记录）
 *   → 负责人记录措施并申请复核（待处置/处置中/待复核）
 *   → 另一名复核人员确认燃烧重新达标且严重告警解除（已恢复，解冻）
 *   → 关闭（已关闭）。
 *
 * 并发口径：
 * - 同炉建单/合并证据：焚烧炉行锁 + 同炉唯一有效单（库级生成列唯一索引兜底），
 *   自动与人工并发到达时合并证据，不重复冻结、不丢失后到严重告警；
 * - 状态推进：@Version 乐观锁拦截重复点击、过期版本、两人同时复核；
 * - 复核窗口：炉行锁 + 告警行锁，恢复期间新到严重告警会驳回恢复并反弹处置单；
 * - 投料联锁：StoragePitService 投料前调 {@link #assertFeedAllowed} 做同炉行锁校验。
 */
@Service
public class AbnormalDisposalService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final AbnormalDisposalRepository disposalRepository;
    private final AbnormalDisposalAlarmRepository linkRepository;
    private final AbnormalDisposalTraceRepository traceRepository;
    private final IncineratorRepository incineratorRepository;
    private final EnvAlarmRepository alarmRepository;
    private final StoragePitRepository pitRepository;
    private final ObjectMapper objectMapper;
    private final TraceWriter traceWriter;

    public AbnormalDisposalService(AbnormalDisposalRepository disposalRepository,
            AbnormalDisposalAlarmRepository linkRepository,
            AbnormalDisposalTraceRepository traceRepository,
            IncineratorRepository incineratorRepository,
            EnvAlarmRepository alarmRepository,
            StoragePitRepository pitRepository,
            ObjectMapper objectMapper,
            TraceWriter traceWriter) {
        this.disposalRepository = disposalRepository;
        this.linkRepository = linkRepository;
        this.traceRepository = traceRepository;
        this.incineratorRepository = incineratorRepository;
        this.alarmRepository = alarmRepository;
        this.pitRepository = pitRepository;
        this.objectMapper = objectMapper;
        this.traceWriter = traceWriter;
    }

    // ============================== 触发（自动 / 人工） ==============================

    /**
     * 自动信号入口：炉温/停留不达标（燃烧判定失败、稳燃被拒）时调用。
     * 独立事务提交，绝不拖垮调用方主流程；同炉已有有效单时仅追加轨迹，不重复冻结。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void signalCombustionAbnormal(String furnaceCode, List<String> reasons) {
        signalCombustionAbnormal(furnaceCode, reasons, null, null);
    }

    /**
     * 炉况自动信号（带本次被判定的炉温/停留覆盖值）：
     * 调用方（如稳燃登记的新炉温被拒）尚未落库，快照仍需体现被判定参数，
     * 但绝不修改焚烧炉本体或任何已完成运行记录。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void signalCombustionAbnormal(String furnaceCode, List<String> reasons,
            BigDecimal tempOverride, BigDecimal residenceOverride) {
        try {
            openInternal(furnaceCode, "AUTO", null, reasons == null ? List.of() : reasons, null, null,
                    tempOverride, residenceOverride);
        } catch (Exception e) {
            // 联锁建立失败必须可见：写一条挂在炉号上的失败轨迹（无单据 id），由人工跟进
            recordOrphanFailure(furnaceCode, "AUTO_SIGNAL", "炉况异常自动建单失败：" + e.getMessage());
        }
    }

    /**
     * 自动信号入口：同炉出现严重排放告警时调用（须在排放登记事务提交后，按告警 id 加载）。
     * 后到严重告警一律挂证据；若处置单已到待复核/已恢复阶段，则反弹为处置中且保持冻结。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void signalSevereAlarm(Long alarmId) {
        EnvAlarm alarm = alarmRepository.findById(alarmId).orElse(null);
        if (alarm == null || !"SEVERE".equals(alarm.getAlarmLevel())) {
            return;
        }
        String stack = alarm.getStackCode() == null || alarm.getStackCode().isBlank()
                ? "STACK-01" : alarm.getStackCode();
        Incinerator furnace = incineratorRepository.findFirstByStackCode(stack).orElse(null);
        if (furnace == null) {
            // 该排放口暂无对应炉号：不自动建单，告警保留，人工发起时可并入证据
            return;
        }
        String furnaceCode = furnace.getFurnaceCode();
        try {
            Incinerator locked = lockFurnace(furnaceCode);
            Optional<AbnormalDisposal> existed = disposalRepository.findActiveByFurnaceCode(furnaceCode);
            if (existed.isPresent()) {
                mergeSevereAlarm(existed.get(), alarm);
                disposalRepository.flush();
                return;
            }
            List<String> reasons = List.of("同炉出现严重排放告警：" + alarmSummary(alarm));
            openInternal(locked, "AUTO", null, reasons, List.of(alarm), "SNAPSHOT", null, null);
        } catch (Exception e) {
            recordOrphanFailure(furnaceCode, "AUTO_SIGNAL", "严重告警自动建单失败（告警 "
                    + alarm.getAlarmNo() + "）：" + e.getMessage());
        }
    }

    /**
     * 人工发起：同炉已有有效单时不另开单，而是把当前严重告警证据合并到既有单并返回该单。
     */
    @Transactional
    public DisposalPayload.VO manualOpen(DisposalPayload.OpenRequest request) {
        String furnaceCode = request.getFurnaceCode();
        if (furnaceCode == null || furnaceCode.isBlank()) {
            throw new BizException("发起处置单必须指定炉号");
        }
        String owner = blankToDefault(request.getOwnerName(), "中控值班");
        List<String> reasons = new ArrayList<>();
        if (request.getReason() != null && !request.getReason().isBlank()) {
            reasons.add(request.getReason().trim());
        } else {
            reasons.add("人工发起异常工况处置");
        }
        Incinerator furnace = lockFurnace(furnaceCode);
        Optional<AbnormalDisposal> existed = disposalRepository.findActiveByFurnaceCode(furnaceCode);
        AbnormalDisposal d;
        boolean merged = false;
        if (existed.isPresent()) {
            d = existed.get();
            // 并发到达（人工晚于自动）：合并证据，不重复冻结
            List<EnvAlarm> active = activeSevereAlarms(furnace, d.getFrozenTime());
            for (EnvAlarm alarm : active) {
                merged |= mergeSevereAlarm(d, alarm);
            }
            trace(d, "MERGE_ALARM", d.getStatus(), d.getStatus(), owner,
                    merged ? "人工发起时合并同炉严重告警证据 " + active.size() + " 条，沿用既有处置单与冻结联锁"
                            : "同炉已有有效处置单 " + d.getDisposalNo() + "，人工发起已合并，不重复冻结", true);
            disposalRepository.save(d);
        } else {
            d = openInternal(furnace, "MANUAL", owner, reasons, activeSevereAlarms(furnace, null), "SNAPSHOT",
                    null, null);
        }
        return toVO(d);
    }

    /**
     * 建单核心：在已持有焚烧炉行锁的前提下执行。快照 + 冻结一次完成，
     * 同炉唯一有效单由 active_once 生成列唯一索引兜底（并发撞单抛约束异常由上层合并/报错）。
     */
    private AbnormalDisposal openInternal(String furnaceCode, String source, String owner,
            List<String> reasons, List<EnvAlarm> alarms, String bindSource,
            BigDecimal tempOverride, BigDecimal residenceOverride) {
        Incinerator furnace = lockFurnace(furnaceCode);
        return openInternal(furnace, source, owner, reasons, alarms, bindSource, tempOverride, residenceOverride);
    }

    private AbnormalDisposal openInternal(Incinerator furnace, String source, String owner,
            List<String> reasons, List<EnvAlarm> alarms, String bindSource,
            BigDecimal tempOverride, BigDecimal residenceOverride) {
        String furnaceCode = furnace.getFurnaceCode();
        Optional<AbnormalDisposal> existed = disposalRepository.findActiveByFurnaceCode(furnaceCode);
        if (existed.isPresent()) {
            // 并发兜底：自动信号晚到时合并证据，不重复冻结
            AbnormalDisposal d = existed.get();
            if (alarms != null) {
                for (EnvAlarm alarm : alarms) {
                    mergeSevereAlarm(d, alarm);
                }
            }
            return disposalRepository.save(d);
        }

        LocalDateTime now = LocalDateTime.now();
        AbnormalDisposal d = new AbnormalDisposal();
        d.setDisposalNo("YCGZ" + now.format(NO_FMT) + furnaceCode.replace("-", ""));
        d.setFurnaceCode(furnaceCode);
        d.setFurnaceName(furnace.getFurnaceName());
        d.setStatus("PENDING");
        d.setTriggerSource(source);
        d.setTriggerReasons(writeReasons(reasons));
        // 炉况快照：被判定参数优先（仅写入快照，不修改炉本体）
        d.setSnapshotFurnaceTempC(tempOverride != null ? tempOverride : furnace.getFurnaceTempC());
        d.setSnapshotResidenceS(residenceOverride != null ? residenceOverride : furnace.getFlueGasResidenceS());
        d.setSnapshotCoPpm(furnace.getCoPpm());
        d.setSnapshotCombustion(furnace.getCombustionStatus());
        d.setSnapshotDailyTon(furnace.getDailyProcessTon());
        d.setSnapshotStackCode(furnace.getStackCode());
        // 供料抓斗快照：炉侧抓斗 + 最近用该抓斗供料的坑位
        d.setSnapshotGrabNo(furnace.getGrabNo());
        if (furnace.getGrabNo() != null) {
            pitRepository.findFirstByGrabNo(furnace.getGrabNo()).ifPresent(pit -> {
                d.setSnapshotPitNo(pit.getPitNo());
                d.setSnapshotPitGrabNo(pit.getGrabNo());
                d.setSnapshotPitStockTon(pit.getCurrentStockTon());
            });
        }
        // 冻结后续投料
        d.setFeedFrozen(true);
        d.setFrozenTime(now);
        d.setOwnerName(owner);
        d.setAlarmDataStatus("AVAILABLE");
        d.setCreateTime(now);
        d.setUpdateTime(now);

        List<EnvAlarm> evidence = alarms;
        if (evidence == null) {
            // 炉况触发时也一并快照当前同炉未解除严重告警，避免只盯炉况遗漏排放联锁
            evidence = activeSevereAlarms(furnace, null);
        }
        // 走到这里时已持有焚烧炉行锁且同炉无有效单，插入不会与并发炎撞（唯一索引仅作库级兜底）
        AbnormalDisposal saved = disposalRepository.saveAndFlush(d);
        LocalDateTime bindTime = LocalDateTime.now();
        for (EnvAlarm alarm : evidence) {
            linkRepository.save(new AbnormalDisposalAlarm(saved.getId(), alarm.getId(),
                    bindSource == null ? "SNAPSHOT" : bindSource, bindTime));
        }
        trace(saved, "CREATE", null, "PENDING", owner,
                (source.equals("AUTO") ? "自动触发" : "人工发起")
                        + "：已保存炉况/告警/供料抓斗快照并冻结该炉后续投料；证据告警 "
                        + evidence.size() + " 条", true);
        return saved;
    }

    // ============================== 状态推进 ==============================

    /** 负责人记录处置措施；可一并申请复核进入待复核 */
    @Transactional
    public DisposalPayload.VO recordMeasure(Long id, DisposalPayload.MeasureRequest request) {
        AbnormalDisposal d = requireDisposal(id);
        checkVersion(d, request.getVersion());
        // 负责人一经指派不随后续记录人变更（另一名复核人的判定始终基于原负责人）
        String owner = d.getOwnerName();
        if (owner == null || owner.isBlank()) {
            owner = blankToDefault(request.getOwnerName(), "中控值班");
        }
        if (request.getMeasureNote() == null || request.getMeasureNote().isBlank()) {
            return failAndThrow(d, "MEASURE", owner, "处置措施不能为空，请记录本次异常处置动作");
        }
        if ("CLOSED".equals(d.getStatus())) {
            return failAndThrow(d, "MEASURE", owner, "处置单已关闭，不能再记录措施");
        }
        if ("REVIEWING".equals(d.getStatus()) && !Boolean.TRUE.equals(request.getRequestReview())) {
            return failAndThrow(d, "MEASURE", owner, "处置单已在待复核，重复提交措施无效，请等待复核结论或由复核人退回");
        }
        String from = d.getStatus();
        LocalDateTime now = LocalDateTime.now();
        d.setOwnerName(owner);
        d.setMeasureNote(request.getMeasureNote().trim());
        if (d.getHandledTime() == null) {
            d.setHandledTime(now);
        }
        String to = from;
        if ("PENDING".equals(from)) {
            to = "HANDLING";
            d.setStatus(to);
        }
        if (Boolean.TRUE.equals(request.getRequestReview())) {
            if ("RECOVERED".equals(d.getStatus()) || "CLOSED".equals(d.getStatus())) {
                return failAndThrow(d, "REQUEST_REVIEW", owner, "处置单已恢复/关闭，无需再申请复核");
            }
            d.setStatus("REVIEWING");
            d.setReviewRequestTime(now);
            to = "REVIEWING";
            trace(d, "REQUEST_REVIEW", from, "REVIEWING", owner, "措施记录完成，申请另一名复核人员复核", true);
        } else {
            trace(d, "MEASURE", from, to, owner, "记录处置措施：" + d.getMeasureNote(), true);
        }
        d.setUpdateTime(now);
        return toVO(saveWithLock(d, owner));
    }

    /** 复核恢复：必须另一名复核人员；燃烧达标且严重告警解除才放行，恢复期间新严重告警反弹 */
    @Transactional
    public DisposalPayload.VO reviewRecover(Long id, DisposalPayload.ReviewRequest request) {
        String reviewer = request.getReviewerName();
        if (reviewer == null || reviewer.isBlank()) {
            throw new BizException("复核恢复必须填写复核人员");
        }
        AbnormalDisposal d = requireDisposal(id);
        checkVersion(d, request.getVersion());
        if (!"REVIEWING".equals(d.getStatus())) {
            return failAndThrow(d, "REVIEW_RECOVER", reviewer,
                    "当前状态为「" + Dict.disposalStatusName(d.getStatus())
                            + "」，仅待复核单据可执行复核恢复，投料保持冻结");
        }
        if (reviewer.equals(d.getOwnerName())) {
            return failAndThrow(d, "REVIEW_RECOVER", reviewer,
                    "复核人员 " + reviewer + " 与处置负责人为同一人，必须由另一名复核人员确认");
        }
        if (!"RECOVER".equals(request.getChoice()) && !"REJECT".equals(request.getChoice())) {
            return failAndThrow(d, "REVIEW_RECOVER", reviewer, "复核结论必须为 RECOVER（恢复）或 REJECT（退回）");
        }

        String note = request.getReviewNote() == null ? "" : request.getReviewNote().trim();

        // 退回：不改变冻结，回到处置中
        if ("REJECT".equals(request.getChoice())) {
            d.setStatus("HANDLING");
            d.setReviewRequestTime(null);
            d.setUpdateTime(LocalDateTime.now());
            trace(d, "REJECT", "REVIEWING", "HANDLING", reviewer,
                    (note.isBlank() ? "复核不通过，退回继续处置（投料保持冻结）" : note), true);
            return toVO(saveWithLock(d, reviewer));
        }

        // 已明确「继续冻结等待补齐」：数据补齐前不评估告警、不放行，且不改变单据/版本
        if ("UNAVAILABLE".equals(d.getAlarmDataStatus()) && "WAIT_FREEZE".equals(d.getMissingChoice())) {
            traceLater(d, "REVIEW_RECOVER", "REVIEWING", "REVIEWING", reviewer,
                    "复核未通过：告警数据暂不可用，已选择继续冻结等待补齐，补齐前不能确认严重告警解除", false);
            throw new BizException("告警数据暂不可用且已选择「继续冻结等待补齐」：数据补齐并确认严重告警解除前，不能复核恢复");
        }

        // 恢复：炉行锁 + 同炉严重告警判定，在同一事务内检查并解冻，防止恢复期间严重告警漏网
        Incinerator furnace = lockFurnace(d.getFurnaceCode());
        LocalDateTime since = d.getFrozenTime() == null ? LocalDateTime.now().minusHours(Dict.SEVERE_ALARM_LOOKBACK_HOURS)
                : d.getFrozenTime();
        List<EnvAlarm> activeSevere = alarmRepository.lockActiveSevere(stackOf(furnace), since);
        List<String> unqualified = unqualifiedItems(furnace);

        if (!activeSevere.isEmpty()) {
            List<Long> alarmIds = activeSevere.stream().map(EnvAlarm::getId).toList();
            List<String> summaries = activeSevere.stream().map(this::alarmSummary).toList();
            // 反弹（证据挂接 + 退回处置中 + 保持冻结）必须独立事务提交：
            // 外层随后抛业务异常回滚，驳回结果不能随回滚丢失，恢复期间反弹告警也不能丢
            traceWriter.reviewBounce(d.getId(), reviewer, note, alarmIds, summaries);
            throw new BizException("复核恢复被驳回：同炉仍有 " + activeSevere.size()
                    + " 条严重告警未解除（恢复期间新告警已反弹回处置中），投料继续冻结");
        }
        if (!unqualified.isEmpty()) {
            traceLater(d, "REVIEW_RECOVER", "REVIEWING", "REVIEWING", reviewer,
                    "复核未通过：燃烧未重新达标（" + String.join("；", unqualified) + "），单据保持待复核", false);
            throw new BizException("燃烧尚未重新达标，不能恢复投料：" + String.join("；", unqualified));
        }

        LocalDateTime now = LocalDateTime.now();
        d.setStatus("RECOVERED");
        d.setReviewerName(reviewer);
        d.setReviewNote(note.isBlank() ? "复核确认：炉膛温度与停留时间重新达标，同炉严重告警已解除" : note);
        d.setRecoverFurnaceTempC(furnace.getFurnaceTempC());
        d.setRecoverResidenceS(furnace.getFlueGasResidenceS());
        d.setFeedFrozen(false);
        d.setUnfrozenTime(now);
        d.setUpdateTime(now);
        // 先提交单据主体（@Version +1；两人同时复核时后提交者在此被乐观锁拦截并留痕），
        // 再用批量 JPQL 解冻同炉其余联锁单，最终状态与投料权限保持一致
        d = saveWithLock(d, reviewer);
        // 解冻同炉其余冻结联锁单（历史「关闭但保持冻结」单在新闭环恢复时一并解除）
        int rows = disposalRepository.unfreezeOtherFurnace(d.getFurnaceCode(), d.getId(), now);
        trace(d, "REVIEW_RECOVER", "REVIEWING", "RECOVERED", reviewer,
                "复核通过并执行恢复，解冻该炉投料（同炉解冻 " + rows + " 张联锁单）", true);
        DisposalPayload.VO vo = toVO(d);
        vo.setFeedFrozen(false);
        vo.setUnfrozenTime(now);
        vo.setFeedBlockedNow(false);
        return vo;
    }

    /** 关闭已恢复处置单 */
    @Transactional
    public DisposalPayload.VO close(Long id, DisposalPayload.CloseRequest request) {
        AbnormalDisposal d = requireDisposal(id);
        checkVersion(d, request.getVersion());
        String operator = blankToDefault(request.getOperatorName(), d.getReviewerName());
        if (!"RECOVERED".equals(d.getStatus())) {
            return failAndThrow(d, "CLOSE", operator,
                    "仅已恢复处置单可关闭，当前为「" + Dict.disposalStatusName(d.getStatus()) + "」");
        }
        LocalDateTime now = LocalDateTime.now();
        d.setStatus("CLOSED");
        d.setCloseTime(now);
        d.setFeedFrozen(false);
        if (d.getUnfrozenTime() == null) {
            d.setUnfrozenTime(now);
        }
        d.setUpdateTime(now);
        trace(d, "CLOSE", "RECOVERED", "CLOSED", operator, "处置单关闭，闭环结束，投料权限正常", true);
        return toVO(saveWithLock(d, operator));
    }

    /**
     * 告警数据暂不可用：必须在「继续冻结等待补齐」与「记录理由后关闭处置但保持投料冻结」之间明确选择。
     * WAIT_FREEZE：状态不动，后续数据补齐后仍按 燃烧达标+严重告警解除 复核恢复；
     * CLOSE_KEEP_FROZEN：单据直接关闭，但 feed_frozen 保持冻结，且该炉不再允许复核恢复，
     *   必须重新发起处置单走完整闭环（恢复条件影响明确可见）。
     */
    @Transactional
    public DisposalPayload.VO chooseWhenAlarmMissing(Long id, DisposalPayload.MissingChoiceRequest request) {
        AbnormalDisposal d = requireDisposal(id);
        checkVersion(d, request.getVersion());
        String operator = blankToDefault(request.getOperatorName(), d.getOwnerName());
        if (!"WAIT_FREEZE".equals(request.getChoice()) && !"CLOSE_KEEP_FROZEN".equals(request.getChoice())) {
            return failAndThrow(d, "DATA_MISSING_CHOICE", operator,
                    "必须明确选择：WAIT_FREEZE（继续冻结等待补齐）或 CLOSE_KEEP_FROZEN（记录理由后关闭但保持冻结）");
        }
        if (request.getReason() == null || request.getReason().isBlank()) {
            return failAndThrow(d, "DATA_MISSING_CHOICE", operator, "告警数据缺失处置必须记录理由");
        }
        if ("CLOSED".equals(d.getStatus())) {
            return failAndThrow(d, "DATA_MISSING_CHOICE", operator, "处置单已关闭，不能再登记数据缺失选择");
        }
        LocalDateTime now = LocalDateTime.now();
        String from = d.getStatus();
        d.setAlarmDataStatus("UNAVAILABLE");
        d.setMissingChoice(request.getChoice());
        d.setMissingReason(request.getReason().trim());
        d.setMissingDecidedBy(operator);
        d.setMissingDecidedTime(now);

        if ("WAIT_FREEZE".equals(request.getChoice())) {
            d.setFeedFrozen(true);
            d.setUpdateTime(now);
            trace(d, "DATA_MISSING_CHOICE", from, from, operator,
                    "告警数据暂不可用，选择继续冻结等待补齐；数据补齐后仍须燃烧达标且严重告警解除方可复核恢复", true);
            return toVO(saveWithLock(d, operator));
        }

        // CLOSE_KEEP_FROZEN：关闭处置流程但不解除联锁
        d.setStatus("CLOSED");
        d.setCloseTime(now);
        d.setFeedFrozen(true);
        d.setUnfrozenTime(null);
        d.setUpdateTime(now);
        trace(d, "DATA_MISSING_CHOICE", from, "CLOSED", operator,
                "告警数据暂不可用，记录理由后关闭处置单，但该炉投料继续冻结；须重新发起处置单并满足完整恢复条件后方可解冻",
                true);
        return toVO(saveWithLock(d, operator));
    }

    // ============================== 查询 ==============================

    @Transactional(readOnly = true)
    public List<DisposalPayload.VO> list(boolean activeOnly) {
        List<AbnormalDisposal> all = activeOnly
                ? disposalRepository.findActiveOrderByCreateTimeDesc()
                : disposalRepository.findTop50ByOrderByCreateTimeDesc();
        Map<String, Integer> evidenceCount = evidenceCount(all);
        return all.stream().map(d -> {
            DisposalPayload.VO vo = toVO(d);
            vo.setAlarmEvidenceCount(evidenceCount.getOrDefault(key(d), 0));
            vo.setFeedBlockedNow(feedBlocked(d.getFurnaceCode()));
            return vo;
        }).toList();
    }

    @Transactional(readOnly = true)
    public DisposalPayload.VO detail(Long id) {
        AbnormalDisposal d = requireDisposal(id);
        DisposalPayload.VO vo = toVO(d);
        vo.setAlarmEvidenceCount(linkRepository.findByDisposalIdOrderByBindTimeDesc(id).size());
        vo.setFeedBlockedNow(feedBlocked(d.getFurnaceCode()));
        return vo;
    }

    @Transactional(readOnly = true)
    public List<DisposalPayload.AlarmEvidenceVO> evidences(Long id) {
        List<AbnormalDisposalAlarm> links = linkRepository.findByDisposalIdOrderByBindTimeDesc(id);
        List<DisposalPayload.AlarmEvidenceVO> result = new ArrayList<>();
        for (AbnormalDisposalAlarm link : links) {
            alarmRepository.findById(link.getAlarmId()).ifPresent(alarm ->
                    result.add(DisposalPayload.AlarmEvidenceVO.from(link, alarm)));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<DisposalPayload.TraceVO> traces(Long id) {
        return traceRepository.findByDisposalIdOrderByOperateTimeDescIdDesc(id).stream()
                .map(DisposalPayload.TraceVO::from).toList();
    }

    /** 实时恢复条件（详情页展示选择影响；与复核恢复接口同一判定口径） */
    @Transactional(readOnly = true)
    public DisposalPayload.ConditionVO condition(Long id) {
        AbnormalDisposal d = requireDisposal(id);
        Incinerator furnace = incineratorRepository.findByFurnaceCode(d.getFurnaceCode())
                .orElseThrow(() -> new BizException("焚烧炉不存在：" + d.getFurnaceCode()));
        LocalDateTime since = d.getFrozenTime() == null ? LocalDateTime.now().minusHours(Dict.SEVERE_ALARM_LOOKBACK_HOURS)
                : d.getFrozenTime();
        List<EnvAlarm> severe = alarmRepository
                .findByStackCodeAndAlarmLevelAndHandledFalseAndAlarmTimeGreaterThanEqualOrderByAlarmTimeDesc(
                        stackOf(furnace), "SEVERE", since);
        List<String> unqualified = unqualifiedItems(furnace);

        DisposalPayload.ConditionVO vo = new DisposalPayload.ConditionVO();
        vo.setFurnaceCode(d.getFurnaceCode());
        vo.setCurrentFurnaceTempC(furnace.getFurnaceTempC());
        vo.setCurrentResidenceS(furnace.getFlueGasResidenceS());
        vo.setCombustionQualified(unqualified.isEmpty());
        vo.setUnqualifiedItems(unqualified);
        vo.setSevereAlarmActive(!severe.isEmpty());
        vo.setSevereAlarmCount(severe.size());
        vo.setAlarmDataStatus(d.getAlarmDataStatus());
        vo.setMissingChoice(d.getMissingChoice());
        vo.setMissingChoiceName(Dict.missingChoiceName(d.getMissingChoice()));
        vo.setFeedBlocked(feedBlocked(d.getFurnaceCode()));

        List<String> blockers = new ArrayList<>();
        if ("CLOSE_KEEP_FROZEN".equals(d.getMissingChoice())) {
            blockers.add("已选择「记录理由后关闭处置但保持投料冻结」：本单不能恢复，须重新发起处置单并满足完整恢复条件");
        }
        if (!unqualified.isEmpty()) {
            blockers.add("燃烧未达标：" + String.join("；", unqualified));
        }
        if ("UNAVAILABLE".equals(d.getAlarmDataStatus()) && "WAIT_FREEZE".equals(d.getMissingChoice())) {
            blockers.add("告警数据暂不可用，正在继续冻结等待补齐，补齐前无法确认严重告警解除");
        } else if (!severe.isEmpty()) {
            blockers.add("同炉仍有 " + severe.size() + " 条严重告警未解除（含恢复期间反弹告警）");
        }
        vo.setBlockers(blockers);
        vo.setCanRecover(blockers.isEmpty() && "REVIEWING".equals(d.getStatus()));
        return vo;
    }

    /** 当前被投料联锁冻结的炉号（炉况入口、储坑投料页据此置灰） */
    @Transactional(readOnly = true)
    public List<String> frozenFurnaceCodes() {
        return disposalRepository.findFrozenActive().stream()
                .map(AbnormalDisposal::getFurnaceCode).distinct().toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dict() {
        Map<String, Object> dict = new LinkedHashMap<>();
        dict.put("statuses", Dict.DISPOSAL_STATUS.stream()
                .map(code -> (Object) Map.of("code", code, "name", Dict.disposalStatusName(code))).toList());
        dict.put("sources", Dict.DISPOSAL_SOURCES.stream()
                .map(code -> (Object) Map.of("code", code, "name", Dict.disposalSourceName(code))).toList());
        dict.put("missingChoices", Dict.MISSING_CHOICES.stream()
                .map(code -> (Object) Map.of(
                        "code", code,
                        "name", Dict.missingChoiceName(code),
                        "recoverImpact", "WAIT_FREEZE".equals(code)
                                ? "状态不变、投料继续冻结；告警数据补齐后，仍须燃烧达标且严重告警解除才能复核恢复"
                                : "处置单直接关闭且投料保持冻结、不能再复核恢复；须重新发起处置单走完整闭环后方可解冻"))
                .toList());
        dict.put("furnaces", incineratorRepository.findAll().stream()
                .map(f -> (Object) Map.of(
                        "furnaceCode", f.getFurnaceCode(),
                        "furnaceName", nvl(f.getFurnaceName(), f.getFurnaceCode()),
                        "stackCode", stackOf(f))).toList());
        return dict;
    }

    // ============================== 投料联锁（供 StoragePitService 调用） ==============================

    /**
     * 投料前校验：目标炉存在未关闭且 feed_frozen=true 的处置单时拒绝投料。
     * 与建单/复核共用焚烧炉行锁，保证冻结与投料严格互斥、状态一致。
     */
    @Transactional
    public void assertFeedAllowed(String furnaceCode) {
        Incinerator furnace = incineratorRepository.lockByFurnaceCode(furnaceCode)
                .orElseThrow(() -> new BizException("目标炉号不存在：" + furnaceCode));
        // 一条查询同时覆盖：处置中/待复核/已恢复的冻结单，以及「关闭处置但保持投料冻结」的 CLOSED 单
        boolean frozen = disposalRepository.findFrozenActive().stream()
                .anyMatch(x -> furnace.getFurnaceCode().equals(x.getFurnaceCode()));
        if (frozen) {
            String extra = disposalRepository.findActiveByFurnaceCode(furnaceCode)
                    .map(x -> "处置单 " + x.getDisposalNo() + "（状态："
                            + Dict.disposalStatusName(x.getStatus()) + "），须由另一名复核人员复核恢复")
                    .orElse("存在按「关闭处置但保持投料冻结」处理的处置单，须重新发起处置并复核恢复");
            throw new BizException("炉号 " + furnaceCode + " 投料已冻结：" + extra + "后方可投料");
        }
    }

    // ============================== 内部方法 ==============================

    private Incinerator lockFurnace(String furnaceCode) {
        return incineratorRepository.lockByFurnaceCode(furnaceCode)
                .orElseThrow(() -> new BizException("焚烧炉不存在：" + furnaceCode));
    }

    private String stackOf(Incinerator furnace) {
        return furnace.getStackCode() == null || furnace.getStackCode().isBlank()
                ? "STACK-01" : furnace.getStackCode();
    }

    /** 同炉未解除严重告警：冻结时点之后（建单时为空则回看窗口内），handled=true 即视为已解除 */
    private List<EnvAlarm> activeSevereAlarms(Incinerator furnace, LocalDateTime frozenTime) {
        LocalDateTime since = frozenTime == null
                ? LocalDateTime.now().minusHours(Dict.SEVERE_ALARM_LOOKBACK_HOURS) : frozenTime;
        return alarmRepository
                .findByStackCodeAndAlarmLevelAndHandledFalseAndAlarmTimeGreaterThanEqualOrderByAlarmTimeDesc(
                        stackOf(furnace), "SEVERE", since);
    }

    /**
     * 合并严重告警证据：返回是否新挂接。反弹规则——待复核/已恢复阶段收到新严重告警：
     * 退回处置中并保持冻结，防止恢复期间告警反弹误放行。
     */
    private boolean mergeSevereAlarm(AbnormalDisposal d, EnvAlarm alarm) {
        if (linkRepository.findByDisposalIdAndAlarmId(d.getId(), alarm.getId()).isPresent()) {
            return false;
        }
        linkRepository.save(new AbnormalDisposalAlarm(d.getId(), alarm.getId(), "MERGED", LocalDateTime.now()));
        String detail = "合并后到严重告警证据：" + alarmSummary(alarm) + "（" + alarm.getAlarmNo() + "）";
        if ("REVIEWING".equals(d.getStatus()) || "RECOVERED".equals(d.getStatus())) {
            String from = d.getStatus();
            d.setStatus("HANDLING");
            d.setReviewRequestTime(null);
            d.setFeedFrozen(true);
            d.setUnfrozenTime(null);
            d.setUpdateTime(LocalDateTime.now());
            trace(d, "MERGE_ALARM", from, "HANDLING", "系统",
                    detail + "；恢复条件被破坏，反弹为处置中并重新冻结投料", true);
        } else {
            trace(d, "MERGE_ALARM", d.getStatus(), d.getStatus(), "系统", detail, true);
        }
        return true;
    }

    private boolean feedBlocked(String furnaceCode) {
        // 覆盖未关闭冻结单与「关闭但保持冻结」单，保证列表状态与投料权限口径一致
        return disposalRepository.findFrozenActive().stream()
                .anyMatch(x -> furnaceCode.equals(x.getFurnaceCode()));
    }

    private AbnormalDisposal requireDisposal(Long id) {
        return disposalRepository.findById(id)
                .orElseThrow(() -> new BizException("异常工况处置单不存在：id=" + id));
    }

    private void checkVersion(AbnormalDisposal d, Integer version) {
        if (version == null) {
            traceWriter.write(d.getId(), d.getDisposalNo(), "FAIL", d.getStatus(), d.getStatus(),
                    "未知", "FAILED", "提交缺少版本号 version，疑似过期页面，已拒绝");
            throw new BizException("提交缺少版本号 version，可能为过期页面，请刷新后重试");
        }
        if (!version.equals(d.getVersion())) {
            traceWriter.write(d.getId(), d.getDisposalNo(), "FAIL", d.getStatus(), d.getStatus(),
                    "未知", "FAILED",
                    "过期版本提交被拒绝：提交 v" + version + "，当前 v" + d.getVersion());
            throw new BizException("处置单已被其他人更新（当前版本 v" + d.getVersion()
                    + "，你提交的是 v" + version + "），请刷新获取最新状态后再操作，本次提交未生效");
        }
    }

    /**
     * 乐观锁冲突保存：重复点击 / 两人同时复核时只有一次成功，另一次留下 FAILED 轨迹并报错。
     * 注意轨迹在独立事务写入，主事务回滚后失败原因仍然保留。
     */
    private AbnormalDisposal saveWithLock(AbnormalDisposal d, String operator) {
        try {
            return disposalRepository.saveAndFlush(d);
        } catch (ObjectOptimisticLockingFailureException e) {
            traceLater(d, "FAIL", d.getStatus(), d.getStatus(), operator,
                    "并发提交冲突（重复点击或他人已先行操作），本次操作未生效", false);
            throw new BizException("操作冲突：处置单刚刚已被其他人更新，请勿重复提交，请刷新后查看最新状态");
        }
    }

    /** 业务条件不满足：先独立事务留痕（FAILED），再抛业务异常，主事务回滚不影响轨迹 */
    private DisposalPayload.VO failAndThrow(AbnormalDisposal d, String action, String operator, String reason) {
        traceLater(d, action, d.getStatus(), d.getStatus(), operator, reason, false);
        throw new BizException(reason);
    }

    // ---- 轨迹：同事务 SUCCESS、独立事务 FAILED ----

    private void trace(AbnormalDisposal d, String action, String from, String to,
            String operator, String detail, boolean success) {
        AbnormalDisposalTrace t = new AbnormalDisposalTrace(d.getId(), d.getDisposalNo(), action,
                from, to, operator, success ? "SUCCESS" : "FAILED", detail, LocalDateTime.now());
        traceRepository.save(t);
    }

    private void traceLater(AbnormalDisposal d, String action, String from, String to,
            String operator, String detail, boolean success) {
        traceWriter.write(d.getId(), d.getDisposalNo(), action, from, to, operator,
                success ? "SUCCESS" : "FAILED", detail);
    }

    private void recordOrphanFailure(String furnaceCode, String action, String detail) {
        try {
            traceWriter.write(0L, "ORPHAN-" + furnaceCode, action, null, null, "系统", "FAILED", detail);
        } catch (Exception ignored) {
            // 轨迹都写不进去时不再上抛，避免影响调用方
        }
    }

    private List<String> unqualifiedItems(Incinerator furnace) {
        List<String> items = new ArrayList<>();
        BigDecimal temp = nvl(furnace.getFurnaceTempC());
        BigDecimal residence = nvl(furnace.getFlueGasResidenceS());
        if (temp.compareTo(Dict.MIN_FURNACE_TEMP_C) < 0) {
            items.add("炉膛温度 " + temp.setScale(1, RoundingMode.HALF_UP) + "℃，低于 "
                    + Dict.MIN_FURNACE_TEMP_C + "℃ 达标线");
        }
        if (residence.compareTo(Dict.MIN_RESIDENCE_S) < 0) {
            items.add("烟气停留时间 " + residence.setScale(2, RoundingMode.HALF_UP) + " s，不足 "
                    + Dict.MIN_RESIDENCE_S + " s");
        }
        return items;
    }

    private String alarmSummary(EnvAlarm alarm) {
        return Dict.indicatorName(alarm.getIndicatorCode()) + " 实测 " + alarm.getMeasuredValue()
                + "，限值 " + alarm.getLimitValue() + "（" + Dict.alarmLevelName("SEVERE") + "）";
    }

    private String writeReasons(List<String> reasons) {
        try {
            return objectMapper.writeValueAsString(reasons == null ? List.of() : reasons);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> readReasons(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of(json);
        }
    }

    private Map<String, Integer> evidenceCount(List<AbnormalDisposal> list) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (AbnormalDisposal d : list) {
            map.put(key(d), linkRepository.findByDisposalIdOrderByBindTimeDesc(d.getId()).size());
        }
        return map;
    }

    private String key(AbnormalDisposal d) {
        return String.valueOf(d.getId());
    }

    private DisposalPayload.VO toVO(AbnormalDisposal d) {
        return DisposalPayload.VO.from(d, readReasons(d.getTriggerReasons()));
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String nvl(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 独立事务写入器：失败的提交/复核在外层业务事务回滚后，轨迹与反弹结果仍然保留。
     * 单独成 bean 以便使用 REQUIRES_NEW 挂起外层事务。
     */
    @org.springframework.stereotype.Component
    public static class TraceWriter {

        private final AbnormalDisposalTraceRepository traceRepository;
        private final AbnormalDisposalRepository disposalRepository;
        private final AbnormalDisposalAlarmRepository linkRepository;

        public TraceWriter(AbnormalDisposalTraceRepository traceRepository,
                AbnormalDisposalRepository disposalRepository,
                AbnormalDisposalAlarmRepository linkRepository) {
            this.traceRepository = traceRepository;
            this.disposalRepository = disposalRepository;
            this.linkRepository = linkRepository;
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void write(Long disposalId, String disposalNo, String action, String from, String to,
                String operator, String result, String detail) {
            traceRepository.save(new AbnormalDisposalTrace(disposalId, disposalNo, action,
                    from, to, operator, result, detail, LocalDateTime.now()));
        }

        /**
         * 复核期间发现未解除严重告警：挂接证据、处置单反弹为处置中、投料保持冻结。
         * 在独立事务中提交，确保外层复核事务回滚（驳回报错）后反弹结果不丢失。
         */
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void reviewBounce(Long disposalId, String reviewer, String reviewNote,
                List<Long> alarmIds, List<String> alarmSummaries) {
            AbnormalDisposal d = disposalRepository.findById(disposalId).orElseThrow();
            LocalDateTime now = LocalDateTime.now();
            int linked = 0;
            for (Long alarmId : alarmIds) {
                if (linkRepository.findByDisposalIdAndAlarmId(disposalId, alarmId).isEmpty()) {
                    linkRepository.save(new AbnormalDisposalAlarm(disposalId, alarmId, "MERGED", now));
                    linked++;
                }
            }
            String from = d.getStatus();
            d.setStatus("HANDLING");
            d.setReviewRequestTime(null);
            d.setReviewerName(reviewer);
            if (reviewNote != null && !reviewNote.isBlank()) {
                d.setReviewNote(reviewNote);
            }
            d.setFeedFrozen(true);
            d.setUnfrozenTime(null);
            d.setUpdateTime(now);
            disposalRepository.saveAndFlush(d);
            traceRepository.save(new AbnormalDisposalTrace(disposalId, d.getDisposalNo(),
                    "REVIEW_RECOVER", from, "HANDLING", reviewer, "FAILED",
                    "恢复期间仍有 " + alarmIds.size() + " 条同炉严重告警未解除（新挂证据 " + linked
                            + " 条：" + String.join("、", alarmSummaries) + "），驳回恢复并反弹处置，投料保持冻结",
                    now));
        }
    }
}
