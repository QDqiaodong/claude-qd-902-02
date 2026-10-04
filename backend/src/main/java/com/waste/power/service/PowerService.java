package com.waste.power.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.waste.power.common.BizException;
import com.waste.power.common.Dict;
import com.waste.power.domain.Boiler;
import com.waste.power.domain.PowerDailySettlement;
import com.waste.power.domain.PowerMeterTrace;
import com.waste.power.domain.TurbineGenerator;
import com.waste.power.dto.PowerPayload;
import com.waste.power.repository.BoilerRepository;
import com.waste.power.repository.PowerDailySettlementRepository;
import com.waste.power.repository.PowerMeterTraceRepository;
import com.waste.power.repository.TurbineGeneratorRepository;

/**
 * 余热锅炉与汽轮发电业务。
 *
 * 发电日结口径（修复点）：登记的是主电能表【当日抄见累计表码】，系统按「机组 + 统计日 + 相邻表码」
 * 计算当日发电增量（期末表码 − 期初基数），再据此计算当日上网电量与厂用电率，
 * 每天的表码、基数来源、表计事件与计算依据都落在 power_daily_settlement，绝不再把累计表码当单日发电量。
 *
 * 表计回拨 / 更换必须二选一并留痕：
 * - SEAL_NEW_BASE 旧表封账并启用新基数：旧表在原有效表码处封账，新表从声明的新基数起算，
 *   后续增量口径锚定新基数（机组当前表号 / 表码同步切换）；
 * - SUSPEND 暂停日结等待校表：当日不出增量，该机组后续日期日结一律拦住，校表确认基数后才能补日结。
 *
 * 并发与重复请求：
 * - 机组行锁（lockByUnitNo）把同机组日结/校表串行化，uk_unit_stat_date 唯一索引库级兜底；
 * - 请求幂等指纹：同机组同日重复提交相同请求直接返回原日结（不产生第二份）；
 * - 同日不同内容（较早页面重复点击、过期页面覆盖）一律拒绝并留痕，已完成日结永不修改；
 * - 校表用 @Version 乐观锁拦截过期页面的重复校表。
 */
@Service
@Transactional
public class PowerService {

    private final BoilerRepository boilerRepository;
    private final TurbineGeneratorRepository turbineRepository;
    private final PowerDailySettlementRepository settlementRepository;
    private final PowerMeterTraceRepository traceRepository;
    private final MeterTraceWriter traceWriter;

    public PowerService(BoilerRepository boilerRepository,
            TurbineGeneratorRepository turbineRepository,
            PowerDailySettlementRepository settlementRepository,
            PowerMeterTraceRepository traceRepository,
            MeterTraceWriter traceWriter) {
        this.boilerRepository = boilerRepository;
        this.turbineRepository = turbineRepository;
        this.settlementRepository = settlementRepository;
        this.traceRepository = traceRepository;
        this.traceWriter = traceWriter;
    }

    // ============================== 锅炉 ==============================

    @Transactional(readOnly = true)
    public List<PowerPayload.BoilerVO> boilers() {
        return boilerRepository.findAll().stream().map(PowerPayload.BoilerVO::from).toList();
    }

    public PowerPayload.BoilerVO adjustBoiler(String boilerNo, PowerPayload.BoilerRequest request) {
        Boiler boiler = boilerRepository.findByBoilerNo(boilerNo)
                .orElseThrow(() -> new BizException("余热锅炉不存在：" + boilerNo));
        if (request.getSteamPressureMpa() != null) {
            boiler.setSteamPressureMpa(request.getSteamPressureMpa());
        }
        if (request.getSteamTempC() != null) {
            boiler.setSteamTempC(request.getSteamTempC());
        }
        if (request.getEvaporationTph() != null) {
            if (request.getEvaporationTph().compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("蒸发量不能为负");
            }
            boiler.setEvaporationTph(request.getEvaporationTph());
        }
        if (request.getStatus() != null) {
            boiler.setStatus(request.getStatus());
        }
        return PowerPayload.BoilerVO.from(boilerRepository.save(boiler));
    }

    // ============================== 机组视图 ==============================

    @Transactional(readOnly = true)
    public List<PowerPayload.TurbineVO> turbines() {
        List<TurbineGenerator> units = turbineRepository.findAll();
        Map<String, PowerDailySettlement> latest = latestSettlementByUnit();
        return units.stream().map(unit -> {
            PowerPayload.TurbineVO vo = toTurbineVO(unit);
            PowerDailySettlement s = latest.get(unit.getUnitNo());
            if (s != null) {
                vo.setAnomalyStatus(s.getAnomalyStatus());
                vo.setAnomalyStatusName(Dict.meterAnomalyName(s.getAnomalyStatus()));
            }
            return vo;
        }).toList();
    }

    // ============================== 发电日结 ==============================

    /**
     * 登记某日发电日结：提交当日主电能表抄见表码，按相邻表码取增量。
     */
    public PowerPayload.DailySettlementVO settle(String unitNo, PowerPayload.DailySettleRequest request) {
        // 机组行锁：同机组所有日结/校表严格串行，重复点击与并发请求在此排队后再做幂等判定
        TurbineGenerator unit = turbineRepository.lockByUnitNo(unitNo)
                .orElseThrow(() -> new BizException("汽轮发电机组不存在：" + unitNo));
        LocalDate statDate = request.getStatDate() == null ? LocalDate.now() : request.getStatDate();
        LocalDate today = LocalDate.now();
        String operator = blankToDefault(request.getOperator(), "中控值班");
        if (statDate.isAfter(today)) {
            laterFail(unitNo, statDate, null, operator, "REJECT_STALE_DATE",
                    "统计日 " + statDate + " 晚于当前日期，不能提前日结");
            throw new BizException("统计日不能晚于当天（" + today + "），不能提前日结");
        }
        BigDecimal current = request.getCurrentReadingKwh();
        if (current == null || current.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("主电能表当日期末抄见表码必须 ≥ 0");
        }
        BigDecimal hours = request.getRunningHours() == null
                ? BigDecimal.valueOf(24) : request.getRunningHours();
        if (hours.compareTo(BigDecimal.ZERO) < 0 || hours.compareTo(BigDecimal.valueOf(24)) > 0) {
            throw new BizException("当日运行小时必须在 0 ~ 24 h 之间");
        }
        String meterNo = blankToDefault(request.getMainMeterNo(), unit.getMeterNo());
        String fingerprint = fingerprint(unitNo, statDate, request);

        PowerDailySettlement existing = settlementRepository.findByUnitNoAndStatDate(unitNo, statDate).orElse(null);

        // 已完成日结：同指纹幂等返回；不同内容（重复点击/较早页面）拒绝，绝不覆盖
        if (existing != null && "SETTLED".equals(existing.getStatus())) {
            return handleExisting(existing, fingerprint, operator);
        }

        // 已暂停待校表：相同请求幂等返回原挂起记录；不同请求一律拒绝，必须先校表
        if (existing != null && "SUSPENDED".equals(existing.getStatus())) {
            if (fingerprint.equals(existing.getRequestFingerprint())) {
                traceWriter.write(unitNo, statDate, existing.getId(), "IDEMPOTENT_RETRY",
                        "SUSPENDED", "SUSPENDED", operator, "SUCCESS",
                        "重复请求与暂停记录口径一致，幂等返回，未产生第二份日结");
                return PowerPayload.DailySettlementVO.from(existing);
            }
            traceWriter.write(unitNo, statDate, existing.getId(), "REJECT_DUPLICATE",
                    "SUSPENDED", "SUSPENDED", operator, "FAILED",
                    "该日已按「暂停日结等待校表」挂起，须先校表确认基数后才能补日结");
            throw new BizException("机组 " + unitNo + " " + statDate
                    + " 已暂停日结等待校表，请先在日结列表登记校表确认，再重新日结");
        }

        // 确定期初基数（相邻表码）
        BigDecimal base;
        String basisSource;
        if (existing != null && "VERIFIED".equals(existing.getStatus())) {
            // 校表后的补日结：基数为校表确认值
            base = existing.getVerifiedReadingKwh();
            basisSource = "VERIFIED_BASE";
        } else {
            List<PowerDailySettlement> earlier = settlementRepository
                    .findByUnitNoAndStatDateLessThanOrderByStatDateDesc(unitNo, statDate);
            PowerDailySettlement prev = earlier.isEmpty() ? null : earlier.get(0);
            if (prev == null) {
                base = request.getInitialBaseReadingKwh();
                if (base == null || base.compareTo(BigDecimal.ZERO) < 0) {
                    throw new BizException("机组 " + unitNo + " 尚无任何日结，首次日结必须提供期初建档基数表码（initialBaseReadingKwh）");
                }
                basisSource = "INIT_BASE";
            } else {
                if (!prev.getStatDate().equals(statDate.minusDays(1))) {
                    laterFail(unitNo, statDate, null, operator, "REJECT_STALE_DATE",
                            "缺 " + prev.getStatDate().plusDays(1) + " ~ " + statDate.minusDays(1)
                                    + " 的日结，跨日跳日结会断表码链");
                    throw new BizException("上一份日结是 " + prev.getStatDate()
                            + "，必须先逐日补齐中间日期的日结，不能跨日跳日结");
                }
                if ("SUSPENDED".equals(prev.getStatus())) {
                    laterFail(unitNo, statDate, prev.getId(), operator, "REJECT_STALE_DATE",
                            "前一统计日 " + prev.getStatDate() + " 暂停日结待校表，增量基数未定");
                    throw new BizException("前一统计日 " + prev.getStatDate()
                            + " 已暂停日结等待校表，须先完成校表与该日补日结，才能结后续日期");
                }
                if (!"SETTLED".equals(prev.getStatus())) {
                    // VERIFIED：已校表但该日尚未补日结，相邻表码（期末增量口径）尚未落定
                    laterFail(unitNo, statDate, prev.getId(), operator, "REJECT_STALE_DATE",
                            "前一统计日 " + prev.getStatDate() + " 已校表但尚未补日结，增量口径未定");
                    throw new BizException("前一统计日 " + prev.getStatDate()
                            + " 已校表但尚未完成补日结，请先补结该日后再结后续日期");
                }
                // 相邻表码：上一日已完成日结（含换表封账日，其期末表码即新表口径）
                base = prev.getCurrentReadingKwh();
                basisSource = "PREV_DAY";
            }
        }

        // 表计回拨 / 更换识别
        String event = request.getMeterEvent() == null ? "" : request.getMeterEvent().trim();
        boolean rollback = current.compareTo(base) < 0;
        if (event.isBlank() && rollback) {
            event = "ROLLBACK";
        }
        if (!event.isBlank() && !"ROLLBACK".equals(event) && !"METER_REPLACED".equals(event)) {
            throw new BizException("表计事件只能是 ROLLBACK（回拨）或 METER_REPLACED（更换）");
        }
        boolean hasEvent = !event.isBlank();
        if (rollback && !hasEvent) {
            event = "ROLLBACK";
            hasEvent = true;
        }
        String decision = request.getDecision() == null ? "" : request.getDecision().trim();
        if (hasEvent && decision.isBlank()) {
            laterFail(unitNo, statDate, existing == null ? null : existing.getId(), operator, "FAIL",
                    "检测到表计" + Dict.meterEventName(event) + "（期初 " + base + " → 期末 " + current
                            + "），未做处置选择");
            throw new BizException("检测到表计" + Dict.meterEventName(event) + "（期初表码 " + base
                    + " → 当日期末 " + current + "），必须明确选择：SEAL_NEW_BASE（旧表封账并启用新基数）"
                    + "或 SUSPEND（暂停日结等待校表），并填写理由");
        }
        if (!decision.isBlank() && !"SEAL_NEW_BASE".equals(decision) && !"SUSPEND".equals(decision)) {
            throw new BizException("表计处置只能是 SEAL_NEW_BASE（旧表封账并启用新基数）或 SUSPEND（暂停日结等待校表）");
        }

        LocalDateTime now = LocalDateTime.now();

        // 选择二：暂停日结等待校表 —— 当日不出增量，挂起直到校表
        if ("SUSPEND".equals(decision)) {
            String reason = requireReason(request.getDecisionReason());
            PowerDailySettlement s = existing == null ? new PowerDailySettlement() : existing;
            String fromStatus = existing == null ? null : existing.getStatus();
            s.setUnitNo(unitNo);
            s.setStatDate(statDate);
            s.setStatus("SUSPENDED");
            s.setMainMeterNo(meterNo);
            s.setPrevReadingKwh(base);
            s.setCurrentReadingKwh(current);
            s.setBasisSource(basisSource);
            s.setGenerationKwh(null);
            s.setGridKwh(null);
            s.setAuxiliaryRate(null);
            s.setRunningHours(hours);
            s.setMeterEvent(event);
            s.setAnomalyStatus("SUSPENDED");
            s.setDecision("SUSPEND");
            s.setDecisionReason(reason);
            s.setDecidedBy(operator);
            s.setDecidedTime(now);
            s.setOperator(operator);
            s.setRequestFingerprint(fingerprint);
            s.setSettleTime(null);
            s.setCreateTime(existing == null ? now : existing.getCreateTime());
            s.setUpdateTime(now);
            PowerDailySettlement saved = settlementRepository.saveAndFlush(s);
            traceWriter.write(unitNo, statDate, saved.getId(), "SUSPEND", fromStatus, "SUSPENDED",
                    operator, "SUCCESS", "表计" + Dict.meterEventName(event) + "：期初表码 " + base
                            + " → 当日期末 " + current + "，选择暂停日结等待校表；当日不出增量，"
                            + "该机组后续日期日结一并挂起，校表确认基数前增量口径冻结");
            return PowerPayload.DailySettlementVO.from(saved);
        }

        // 计算当日发电增量
        BigDecimal generation;
        String settledMeterNo = meterNo;
        BigDecimal settledPrev = base;
        String settledBasis = basisSource;
        BigDecimal oldSealed = null;
        String newMeterNo = null;
        BigDecimal newMeterBase = null;
        String anomaly = "NORMAL";

        if ("SEAL_NEW_BASE".equals(decision)) {
            // 选择一：旧表封账并启用新基数。旧表在原有效表码（期初基数）处封账，新表从声明基数起算
            String reason = requireReason(request.getDecisionReason());
            newMeterBase = request.getNewMeterBaseReadingKwh() == null
                    ? BigDecimal.ZERO : request.getNewMeterBaseReadingKwh();
            if (newMeterBase.compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("新表启用基数不能为负");
            }
            if ("METER_REPLACED".equals(event)) {
                newMeterNo = request.getNewMeterNo();
                if (newMeterNo == null || newMeterNo.isBlank()) {
                    throw new BizException("更换表计必须登记新表表号");
                }
                newMeterNo = newMeterNo.trim();
            } else {
                // 回拨后原表重新定底：表号沿用当前表号（也可显式给新表号）
                newMeterNo = request.getNewMeterNo() == null || request.getNewMeterNo().isBlank()
                        ? meterNo : request.getNewMeterNo().trim();
            }
            oldSealed = base.setScale(2, RoundingMode.HALF_UP);
            generation = current.subtract(newMeterBase).setScale(2, RoundingMode.HALF_UP);
            if (generation.compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("新表当日期末表码 " + current + " 低于启用基数 " + newMeterBase
                        + "，新基数口径下增量为负，请核对新表基数或改选暂停日结等待校表");
            }
            settledMeterNo = newMeterNo;
            settledPrev = newMeterBase;
            settledBasis = "NEW_METER_BASE";
            anomaly = "METER_REPLACED".equals(event) ? "METER_REPLACED" : "METER_ROLLBACK";
            // 校验理论上限后再落库
            assertWithinCapacity(unit, generation, hours);
            PowerDailySettlement s = fillSettled(existing, unitNo, statDate, settledMeterNo, settledPrev, current,
                    settledBasis, generation, hours, request, anomaly, event, oldSealed, newMeterNo, newMeterBase,
                    reason, operator, fingerprint, now);
            applyToUnit(unit, s, current);
            PowerDailySettlement saved = settlementRepository.saveAndFlush(s);
            turbineRepository.save(unit);
            traceWriter.write(unitNo, statDate, saved.getId(), "SEAL_NEW_BASE",
                    existing == null ? null : existing.getStatus(), "SETTLED", operator, "SUCCESS",
                    "表计" + Dict.meterEventName(event) + "：旧表在 " + oldSealed + " kWh 封账，新表 "
                            + newMeterNo + " 启用基数 " + newMeterBase + " kWh；当日增量 = 期末 " + current
                            + " − 新基数 " + newMeterBase + " = " + generation + " kWh，后续增量按新基数口径计算");
            return PowerPayload.DailySettlementVO.from(saved);
        }

        // 常规日结：当日增量 = 期末表码 − 相邻表码
        generation = current.subtract(base).setScale(2, RoundingMode.HALF_UP);
        if (generation.compareTo(BigDecimal.ZERO) < 0) {
            // 理论上上面已按回拨拦截，兜底防止负数增量
            throw new BizException("当日发电增量为负（期末 " + current + " < 期初 " + base
                    + "），疑似表计回拨，请按表计异常处置后再日结");
        }
        assertWithinCapacity(unit, generation, hours);
        PowerDailySettlement s = fillSettled(existing, unitNo, statDate, settledMeterNo, settledPrev, current,
                settledBasis, generation, hours, request, "NORMAL", "NONE", null, null, null, null,
                operator, fingerprint, now);
        applyToUnit(unit, s, current);
        PowerDailySettlement saved = settlementRepository.saveAndFlush(s);
        turbineRepository.save(unit);
        traceWriter.write(unitNo, statDate, saved.getId(), "SETTLE",
                existing == null ? null : "VERIFIED", "SETTLED", operator, "SUCCESS",
                Dict.basisSourceName(settledBasis) + "：期初表码 " + settledPrev + " → 期末表码 " + current
                        + "，当日发电增量 " + generation + " kWh；当日上网电量 " + s.getGridKwh()
                        + " kWh，厂用电率 " + s.getAuxiliaryRate() + "%");
        return PowerPayload.DailySettlementVO.from(saved);
    }

    /**
     * 暂停日结后的现场校表：登记确认的有效基数，SUSPENDED → VERIFIED。
     * 之后对同一统计日重新日结时按校表确认基数（VERIFIED_BASE）计算增量。
     */
    public PowerPayload.DailySettlementVO verify(Long id, PowerPayload.MeterVerifyRequest request) {
        PowerDailySettlement s = settlementRepository.findById(id)
                .orElseThrow(() -> new BizException("日结记录不存在：id=" + id));
        final String unitNo = s.getUnitNo();
        // 机组行锁与日结互斥，保证校表与并发日结严格串行
        TurbineGenerator unit = turbineRepository.lockByUnitNo(unitNo)
                .orElseThrow(() -> new BizException("汽轮发电机组不存在：" + unitNo));
        String operator = blankToDefault(request.getOperator(), "校表员");
        if (request.getVersion() == null) {
            traceWriter.write(unitNo, s.getStatDate(), id, "FAIL", s.getStatus(), s.getStatus(),
                    operator, "FAILED", "校表提交缺少版本号，疑似过期页面，已拒绝");
            throw new BizException("校表提交缺少版本号，可能为过期页面，请刷新后重试");
        }
        if (!"SUSPENDED".equals(s.getStatus())) {
            traceWriter.write(unitNo, s.getStatDate(), id, "FAIL", s.getStatus(), s.getStatus(),
                    operator, "FAILED", "仅暂停待校表的日结可登记校表，当前为 " + s.getStatus());
            throw new BizException("仅「暂停待校表」的日结记录可以登记校表，当前状态为「"
                    + Dict.settlementStatusName(s.getStatus()) + "」");
        }
        if (!request.getVersion().equals(s.getVersion())) {
            traceWriter.write(unitNo, s.getStatDate(), id, "FAIL", s.getStatus(), s.getStatus(),
                    operator, "FAILED", "过期版本校表被拒：提交 v" + request.getVersion()
                            + "，当前 v" + s.getVersion());
            throw new BizException("该校表记录已被其他人更新（当前 v" + s.getVersion() + "，你提交的是 v"
                    + request.getVersion() + "），请刷新后重试");
        }
        BigDecimal confirmed = request.getConfirmedReadingKwh();
        if (confirmed == null || confirmed.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("校表确认的有效基数表码必须 ≥ 0");
        }
        LocalDateTime now = LocalDateTime.now();
        String from = s.getStatus();
        s.setStatus("VERIFIED");
        s.setAnomalyStatus("SUSPENDED");
        s.setVerifiedReadingKwh(confirmed.setScale(2, RoundingMode.HALF_UP));
        s.setVerifiedBy(operator);
        s.setVerifiedTime(now);
        s.setVerifyNote(request.getVerifyNote());
        s.setUpdateTime(now);
        try {
            s = settlementRepository.saveAndFlush(s);
        } catch (ObjectOptimisticLockingFailureException e) {
            traceWriter.write(s.getUnitNo(), s.getStatDate(), id, "FAIL", from, from, operator, "FAILED",
                    "校表并发冲突（重复点击或他人已先行校表），本次未生效");
            throw new BizException("校表冲突：该记录刚被其他人更新，请勿重复提交，请刷新查看最新状态");
        }
        traceWriter.write(s.getUnitNo(), s.getStatDate(), s.getId(), "VERIFY", from, "VERIFIED", operator,
                "SUCCESS", "现场校表确认有效基数 " + confirmed + " kWh；请对 " + s.getStatDate()
                        + " 重新日结，增量将按校表确认基数（VERIFIED_BASE）计算，后续日期日结随之解锁");
        return PowerPayload.DailySettlementVO.from(s);
    }

    // ============================== 查询 ==============================

    @Transactional(readOnly = true)
    public List<PowerPayload.DailySettlementVO> settlements(String unitNo) {
        Map<String, String> unitNames = turbineRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(TurbineGenerator::getUnitNo,
                        t -> t.getUnitName() == null ? t.getUnitNo() : t.getUnitName(), (a, b) -> a,
                        LinkedHashMap::new));
        List<PowerDailySettlement> list = unitNo == null || unitNo.isBlank()
                ? settlementRepository.findTop80ByOrderByStatDateDescUpdateTimeDesc()
                : settlementRepository.findByUnitNoOrderByStatDateDesc(unitNo);
        return list.stream().map(s -> {
            PowerPayload.DailySettlementVO vo = PowerPayload.DailySettlementVO.from(s);
            vo.setUnitName(unitNames.getOrDefault(s.getUnitNo(), s.getUnitNo()));
            return vo;
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<PowerPayload.MeterTraceVO> traces(Long settlementId) {
        return traceRepository.findBySettlementIdOrderByOperateTimeDescIdDesc(settlementId).stream()
                .map(PowerPayload.MeterTraceVO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PowerPayload.MeterTraceVO> unitTraces(String unitNo) {
        return traceRepository.findTop30ByUnitNoOrderByOperateTimeDescIdDesc(unitNo).stream()
                .map(PowerPayload.MeterTraceVO::from).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dict() {
        Map<String, Object> dict = new LinkedHashMap<>();
        dict.put("boilerStatuses", Dict.BOILER_STATUS.stream()
                .map(code -> (Object) Map.of("code", code, "name", Dict.boilerStatusName(code)))
                .toList());
        dict.put("unitStatuses", Dict.UNIT_STATUS.stream()
                .map(code -> (Object) Map.of("code", code, "name", Dict.unitStatusName(code)))
                .toList());
        dict.put("settlementStatuses", Dict.SETTLEMENT_STATUS.stream()
                .map(code -> (Object) Map.of("code", code, "name", Dict.settlementStatusName(code)))
                .toList());
        dict.put("basisSources", Dict.BASIS_SOURCES.stream()
                .map(code -> (Object) Map.of("code", code, "name", Dict.basisSourceName(code)))
                .toList());
        dict.put("meterEvents", Dict.METER_EVENTS.stream()
                .map(code -> (Object) Map.of("code", code, "name", Dict.meterEventName(code)))
                .toList());
        dict.put("anomalyStatuses", Dict.METER_ANOMALIES.stream()
                .map(code -> (Object) Map.of("code", code, "name", Dict.meterAnomalyName(code)))
                .toList());
        dict.put("meterDecisions", Dict.METER_DECISIONS.stream()
                .map(code -> (Object) Map.of(
                        "code", code,
                        "name", Dict.meterDecisionName(code),
                        "incrementImpact", "SEAL_NEW_BASE".equals(code)
                                ? "旧表当日封账，新表从声明基数起算，当日增量=新表期末−新基数，后续日期沿用新基数口径"
                                : "当日不出增量并挂起，该机组后续日期日结全部暂停，校表确认基数后方可补日结"))
                .toList());
        dict.put("defaultGridRate", Dict.DEFAULT_GRID_RATE);
        dict.put("maxAuxiliaryRate", Dict.MAX_AUXILIARY_RATE);
        return dict;
    }

    // ============================== 内部方法 ==============================

    /** 已完成日结的重复请求：同指纹幂等返回，不同内容拒绝且留痕（较早页面不得改掉较新结果） */
    private PowerPayload.DailySettlementVO handleExisting(PowerDailySettlement existing, String fingerprint,
            String operator) {
        if (fingerprint.equals(existing.getRequestFingerprint())) {
            traceWriter.write(existing.getUnitNo(), existing.getStatDate(), existing.getId(), "IDEMPOTENT_RETRY",
                    "SETTLED", "SETTLED", operator, "SUCCESS",
                    "重复请求与已完成日结口径一致，幂等返回原日结，未产生第二份日结");
            return PowerPayload.DailySettlementVO.from(existing);
        }
        traceWriter.write(existing.getUnitNo(), existing.getStatDate(), existing.getId(), "REJECT_DUPLICATE",
                "SETTLED", "SETTLED", operator, "FAILED",
                existing.getUnitNo() + " " + existing.getStatDate() + " 已完成日结（期末表码 "
                        + existing.getCurrentReadingKwh() + "，当日增量 " + existing.getGenerationKwh()
                        + "），较早页面发出的不同请求不得改掉较新的日结结果");
        throw new BizException(existing.getUnitNo() + " " + existing.getStatDate()
                + " 的日结已完成，不能重复日结或用过期页面覆盖（请刷新页面查看最新结果）");
    }

    private PowerDailySettlement fillSettled(PowerDailySettlement existing, String unitNo, LocalDate statDate,
            String meterNo, BigDecimal prevReading, BigDecimal current, String basisSource, BigDecimal generation,
            BigDecimal hours, PowerPayload.DailySettleRequest request, String anomaly, String event,
            BigDecimal oldSealed, String newMeterNo, BigDecimal newMeterBase, String decisionReason,
            String operator, String fingerprint, LocalDateTime now) {
        PowerDailySettlement s = existing == null ? new PowerDailySettlement() : existing;
        s.setUnitNo(unitNo);
        s.setStatDate(statDate);
        s.setStatus("SETTLED");
        s.setMainMeterNo(meterNo);
        s.setPrevReadingKwh(prevReading.setScale(2, RoundingMode.HALF_UP));
        s.setCurrentReadingKwh(current.setScale(2, RoundingMode.HALF_UP));
        s.setBasisSource(basisSource);
        s.setGenerationKwh(generation);
        s.setRunningHours(hours);
        s.setMeterEvent(event);
        s.setAnomalyStatus(anomaly);
        s.setOldMeterSealedReadingKwh(oldSealed);
        s.setNewMeterNo(newMeterNo);
        s.setNewMeterBaseReadingKwh(newMeterBase);
        if ("SEAL_NEW_BASE".equals(request.getDecision())) {
            s.setDecision("SEAL_NEW_BASE");
            s.setDecisionReason(decisionReason);
            s.setDecidedBy(operator);
            s.setDecidedTime(now);
        } else {
            s.setDecision(null);
            s.setDecisionReason(null);
            s.setDecidedBy(null);
            s.setDecidedTime(null);
        }
        // 校表补日结后清掉挂起/校表标记
        s.setVerifiedReadingKwh(null);
        s.setVerifiedBy(null);
        s.setVerifiedTime(null);
        s.setVerifyNote(null);
        s.setOperator(operator);
        s.setRequestFingerprint(fingerprint);
        s.setSettleTime(now);
        s.setUpdateTime(now);
        if (existing == null) {
            s.setCreateTime(now);
        }

        // 上网电量：关口表表码差值优先，其次直接填报，最后按缺省比例估算
        BigDecimal grid;
        BigDecimal gridPrev = request.getGridPrevReadingKwh();
        BigDecimal gridCur = request.getGridCurrentReadingKwh();
        if (gridPrev != null || gridCur != null) {
            if (gridPrev == null || gridCur == null) {
                throw new BizException("上网关口表期初/期末表码必须同时提供");
            }
            if (gridCur.compareTo(gridPrev) < 0) {
                throw new BizException("上网关口表当日期末表码 " + gridCur + " 小于期初 " + gridPrev
                        + "，请先核对关口表");
            }
            grid = gridCur.subtract(gridPrev).setScale(2, RoundingMode.HALF_UP);
            s.setGridPrevReadingKwh(gridPrev.setScale(2, RoundingMode.HALF_UP));
            s.setGridCurrentReadingKwh(gridCur.setScale(2, RoundingMode.HALF_UP));
        } else if (request.getGridKwh() != null) {
            grid = request.getGridKwh().setScale(2, RoundingMode.HALF_UP);
            s.setGridPrevReadingKwh(null);
            s.setGridCurrentReadingKwh(null);
        } else {
            grid = generation.multiply(Dict.DEFAULT_GRID_RATE).setScale(2, RoundingMode.HALF_UP);
            s.setGridPrevReadingKwh(null);
            s.setGridCurrentReadingKwh(null);
        }
        if (grid.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("当日上网电量不能为负");
        }
        if (grid.compareTo(generation) > 0) {
            throw new BizException("当日上网电量 " + grid + " kWh 不得大于当日发电增量 " + generation + " kWh");
        }
        s.setGridKwh(grid);
        // 厂用电率 = (发电增量 − 上网电量) / 发电增量 × 100
        if (generation.compareTo(BigDecimal.ZERO) > 0) {
            s.setAuxiliaryRate(generation.subtract(grid)
                    .divide(generation, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP));
        } else {
            s.setAuxiliaryRate(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
        return s;
    }

    /** 日结完成后把机组快照更新为最近统计日口径 */
    private void applyToUnit(TurbineGenerator unit, PowerDailySettlement s, BigDecimal current) {
        unit.setGenerationKwh(s.getGenerationKwh());
        unit.setGridKwh(s.getGridKwh());
        unit.setAuxiliaryRate(s.getAuxiliaryRate());
        unit.setRunningHours(s.getRunningHours());
        unit.setMainMeterReadingKwh(s.getCurrentReadingKwh());
        unit.setMeterNo(s.getMainMeterNo());
        unit.setBasisSource(s.getBasisSource());
        if (s.getGridCurrentReadingKwh() != null) {
            unit.setGridMeterReadingKwh(s.getGridCurrentReadingKwh());
        }
        unit.setStatDate(s.getStatDate());
        unit.setStatus("ON_GRID");
    }

    /** 理论上限 = 装机 MW × 1000 × 当日运行小时 h，当日增量超限时拒绝 */
    private void assertWithinCapacity(TurbineGenerator unit, BigDecimal generation, BigDecimal hours) {
        BigDecimal maxKwh = nvl(unit.getCapacityMw())
                .multiply(BigDecimal.valueOf(1000))
                .multiply(hours)
                .setScale(2, RoundingMode.HALF_UP);
        if (generation.compareTo(maxKwh) > 0) {
            throw new BizException("当日发电增量 " + generation.setScale(2, RoundingMode.HALF_UP)
                    + " kWh 超过理论上限 " + maxKwh + " kWh（装机 " + unit.getCapacityMw()
                    + " MW × 当日运行 " + hours + " h），请核对表码");
        }
    }

    private Map<String, PowerDailySettlement> latestSettlementByUnit() {
        Map<String, PowerDailySettlement> map = new LinkedHashMap<>();
        for (PowerDailySettlement s : settlementRepository.findTop80ByOrderByStatDateDescUpdateTimeDesc()) {
            map.putIfAbsent(s.getUnitNo(), s);
        }
        return map;
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BizException("表计回拨/更换的处置必须填写理由并留痕");
        }
        return reason.trim();
    }

    private void laterFail(String unitNo, LocalDate statDate, Long sid, String operator, String action, String detail) {
        traceWriter.write(unitNo, statDate, sid, action, null, null, operator, "FAILED", detail);
    }

    /** 请求幂等指纹：同机组同日同口径的重复请求识别为同一份（忽略操作人姓名差异） */
    private String fingerprint(String unitNo, LocalDate statDate, PowerPayload.DailySettleRequest r) {
        String raw = String.join("|",
                unitNo,
                statDate.toString(),
                str(r.getCurrentReadingKwh()),
                nz(r.getMainMeterNo()),
                str(r.getInitialBaseReadingKwh()),
                str(r.getRunningHours()),
                str(r.getGridKwh()),
                str(r.getGridPrevReadingKwh()),
                str(r.getGridCurrentReadingKwh()),
                nz(r.getDecision()),
                nz(r.getMeterEvent()),
                nz(r.getNewMeterNo()),
                str(r.getNewMeterBaseReadingKwh()));
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(raw.hashCode());
        }
    }

    private String str(BigDecimal v) {
        return v == null ? "" : v.stripTrailingZeros().toPlainString();
    }

    private String nz(String v) {
        return v == null ? "" : v.trim();
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private PowerPayload.TurbineVO toTurbineVO(TurbineGenerator unit) {
        BigDecimal capacity = nvl(unit.getCapacityMw());
        BigDecimal hours = nvl(unit.getRunningHours());
        BigDecimal maxKwh = capacity.multiply(BigDecimal.valueOf(1000)).multiply(hours)
                .setScale(2, RoundingMode.HALF_UP);
        double factor = maxKwh.compareTo(BigDecimal.ZERO) == 0 ? 0D
                : nvl(unit.getGenerationKwh()).doubleValue() / maxKwh.doubleValue();
        return PowerPayload.TurbineVO.from(unit, maxKwh, BigDecimal.valueOf(factor)
                .setScale(4, RoundingMode.HALF_UP).doubleValue());
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** 独立事务轨迹写入器：被拒绝的重复请求 / 过期页面提交在主事务回滚后仍然留痕 */
    @org.springframework.stereotype.Component
    public static class MeterTraceWriter {

        private final PowerMeterTraceRepository traceRepository;

        public MeterTraceWriter(PowerMeterTraceRepository traceRepository) {
            this.traceRepository = traceRepository;
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void write(String unitNo, LocalDate statDate, Long settlementId, String action,
                String fromStatus, String toStatus, String operator, String result, String detail) {
            String safeDetail = detail == null ? null : detail.substring(0, Math.min(detail.length(), 500));
            traceRepository.save(new PowerMeterTrace(unitNo, statDate, settlementId, action, fromStatus,
                    toStatus, operator, result, safeDetail, LocalDateTime.now()));
        }
    }
}
