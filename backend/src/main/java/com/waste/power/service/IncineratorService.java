package com.waste.power.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.waste.power.common.BizException;
import com.waste.power.common.Dict;
import com.waste.power.domain.FurnaceRunLog;
import com.waste.power.domain.Incinerator;
import com.waste.power.dto.FurnaceMonthlyDTO;
import com.waste.power.dto.FurnacePayload;
import com.waste.power.dto.FurnaceTypeStatDTO;
import com.waste.power.repository.FurnaceRunLogRepository;
import com.waste.power.repository.IncineratorRepository;

/**
 * 焚烧炉与炉型业务：
 * - 单表继承体系（炉排炉 / 流化床 / 回转窑）由 @DiscriminatorFormula 判别子类；
 * - 燃烧工况达标判定（炉膛温度 ≥850℃ 且烟气停留 ≥2s）；
 * - 启炉 / 稳燃 / 停炉 状态推进，并写入启停炉记录。
 */
@Service
@Transactional
public class IncineratorService {

    private final IncineratorRepository incineratorRepository;
    private final FurnaceRunLogRepository runLogRepository;
    private final AbnormalDisposalService disposalService;

    public IncineratorService(IncineratorRepository incineratorRepository, FurnaceRunLogRepository runLogRepository,
            AbnormalDisposalService disposalService) {
        this.incineratorRepository = incineratorRepository;
        this.runLogRepository = runLogRepository;
        this.disposalService = disposalService;
    }

    /**
     * 全部焚烧炉（按子类携带专属参数）
     */
    @Transactional(readOnly = true)
    public List<FurnacePayload.VO> list() {
        return incineratorRepository.findAll().stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional(readOnly = true)
    public FurnacePayload.VO get(String furnaceCode) {
        return toVO(requireFurnace(furnaceCode));
    }

    /**
     * 燃烧工况达标判定：炉膛温度 ≥850℃ 且烟气停留 ≥2s，否则抛出具体不达标项。
     * 不达标即自动触发异常工况处置单（快照 + 冻结投料），达标判定本身不解除任何处置联锁
     * （解冻必须由另一名复核人员走复核恢复）。
     */
    public Map<String, Object> judge(String furnaceCode) {
        Incinerator furnace = requireFurnace(furnaceCode);
        List<String> items = unqualifiedItems(furnace);
        if (!items.isEmpty()) {
            // 自动触发：独立事务建单，不影响本次判定报错；同炉已有有效单时只追加/合并，不重复冻结
            disposalService.signalCombustionAbnormal(furnaceCode,
                    new ArrayList<>(items).stream().map(it -> "燃烧判定不达标：" + it).toList());
            throw new BizException("炉号 " + furnace.getFurnaceCode() + " 燃烧工况未达标：" + String.join("；", items)
                    + "；已自动开立异常工况处置单并冻结该炉投料");
        }
        furnace.setCombustionStatus("STABLE");
        Incinerator saved = incineratorRepository.save(furnace);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("furnaceCode", saved.getFurnaceCode());
        result.put("qualified", true);
        result.put("message", "炉号 " + saved.getFurnaceCode() + " 炉膛温度 "
                + saved.getFurnaceTempC() + "℃、烟气停留 " + saved.getFlueGasResidenceS()
                + "s，判定为达标燃烧，状态置为稳燃");
        return result;
    }

    /**
     * 启炉 / 稳燃 / 停炉 状态推进，并登记启停炉记录
     */
    public FurnacePayload.VO run(String furnaceCode, FurnacePayload.RunRequest request) {
        Incinerator furnace = requireFurnace(furnaceCode);
        String action = request.getAction();
        if (!Dict.isValidFurnaceAction(action)) {
            throw new BizException("启停炉动作不合法：" + action + "（应为 START / STABILIZE / STOP）");
        }
        if (request.getFurnaceTempC() != null) {
            furnace.setFurnaceTempC(request.getFurnaceTempC());
        }
        if (request.getFlueGasResidenceS() != null) {
            furnace.setFlueGasResidenceS(request.getFlueGasResidenceS());
        }
        BigDecimal processTon = request.getProcessTon();
        BigDecimal runHours = request.getRunHours();
        LocalDateTime now = LocalDateTime.now();

        switch (action) {
            case "START" -> {
                furnace.setCombustionStatus("STARTING");
                furnace.setLastStartTime(now);
            }
            case "STABILIZE" -> {
                // 稳燃必须先满足达标判定条件
                List<String> items = unqualifiedItems(furnace);
                if (!items.isEmpty()) {
                    disposalService.signalCombustionAbnormal(furnaceCode,
                            items.stream().map(it -> "稳燃判定不达标：" + it).toList(),
                            furnace.getFurnaceTempC(), furnace.getFlueGasResidenceS());
                    throw new BizException("炉号 " + furnace.getFurnaceCode() + " 不允许判定为达标燃烧："
                            + String.join("；", items) + "；已自动开立异常工况处置单并冻结该炉投料");
                }
                furnace.setCombustionStatus("STABLE");
            }
            case "STOP" -> {
                furnace.setCombustionStatus("SHUTDOWN");
            }
            default -> throw new BizException("启停炉动作不合法：" + action);
        }

        if (processTon != null && processTon.compareTo(BigDecimal.ZERO) > 0) {
            furnace.setDailyProcessTon(nvl(furnace.getDailyProcessTon()).add(processTon));
        }
        if (runHours != null && runHours.compareTo(BigDecimal.ZERO) > 0) {
            furnace.setRunningHours(nvl(furnace.getRunningHours()).add(runHours));
        }

        FurnaceRunLog log = new FurnaceRunLog();
        log.setFurnaceCode(furnace.getFurnaceCode());
        log.setAction(action);
        log.setOperateTime(now);
        log.setPeriodMonth(now.format(DateTimeFormatter.ofPattern("yyyy-MM")));
        log.setFurnaceTempC(furnace.getFurnaceTempC());
        log.setProcessTon(processTon == null ? BigDecimal.ZERO : processTon);
        // 默认值放到 service：未填当班发电量时按 吨垃圾折算电量 估算
        log.setPowerKwh(processTon == null ? BigDecimal.ZERO
                : processTon.multiply(Dict.POWER_PER_TON_KWH).setScale(2, RoundingMode.HALF_UP));
        log.setRunHours(runHours == null ? BigDecimal.ZERO : runHours);
        log.setOperator(request.getOperator() == null || request.getOperator().isBlank()
                ? "中控值班" : request.getOperator());
        log.setRemark(request.getRemark());
        runLogRepository.save(log);

        return toVO(incineratorRepository.save(furnace));
    }

    /**
     * 【JPQL TYPE()】按焚烧炉子类统计：台数、当日入炉量、平均炉膛温度、额定处理能力
     */
    @Transactional(readOnly = true)
    public List<FurnaceTypeStatDTO> statsByType() {
        List<FurnaceTypeStatDTO> stats = new ArrayList<>();
        for (Object[] row : incineratorRepository.statsByType()) {
            String type = typeCodeOf(row[0]);
            Long count = row[1] == null ? 0L : ((Number) row[1]).longValue();
            BigDecimal dailyTon = row[2] == null ? BigDecimal.ZERO : toBigDecimal(row[2]);
            Double avgTemp = row[3] == null ? 0D : ((Number) row[3]).doubleValue();
            BigDecimal rated = row[4] == null ? BigDecimal.ZERO : toBigDecimal(row[4]);
            stats.add(new FurnaceTypeStatDTO(type, Dict.furnaceTypeName(type), count,
                    dailyTon.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(avgTemp).setScale(1, RoundingMode.HALF_UP).doubleValue(),
                    rated.setScale(2, RoundingMode.HALF_UP)));
        }
        stats.sort((a, b) -> a.getFurnaceType().compareTo(b.getFurnaceType()));
        return stats;
    }

    /**
     * 【JPQL】按炉号统计月度处理量与发电量
     */
    @Transactional(readOnly = true)
    public List<FurnaceMonthlyDTO> monthlyStats() {
        return runLogRepository.monthlyStatsByFurnace();
    }

    /**
     * 【JPQL treat()】炉排速度不低于给定速度的炉排炉
     */
    @Transactional(readOnly = true)
    public List<FurnacePayload.VO> fastGrateFurnaces(BigDecimal speed) {
        return incineratorRepository.findFastGrateFurnaces(speed).stream().map(this::toVO).toList();
    }

    @Transactional(readOnly = true)
    public List<FurnacePayload.LogVO> logs(String furnaceCode) {
        if (furnaceCode == null || furnaceCode.isBlank()) {
            return runLogRepository.findTop30ByOrderByOperateTimeDesc().stream()
                    .map(FurnacePayload.LogVO::from).toList();
        }
        return runLogRepository.findTop10ByFurnaceCodeOrderByOperateTimeDesc(furnaceCode).stream()
                .map(FurnacePayload.LogVO::from).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dict() {
        Map<String, Object> dict = new LinkedHashMap<>();
        dict.put("furnaceTypes", Dict.FURNACE_TYPES.stream()
                .map(code -> (Object) Map.of("code", code, "name", Dict.furnaceTypeName(code),
                        "range", Dict.furnaceTypeRange(code)))
                .toList());
        dict.put("combustionStatuses", Dict.COMBUSTION_STATUS.stream()
                .map(code -> (Object) Map.of("code", code, "name", Dict.combustionStatusName(code)))
                .toList());
        dict.put("actions", Dict.FURNACE_ACTIONS.stream()
                .map(code -> (Object) Map.of("code", code, "name", Dict.furnaceActionName(code)))
                .toList());
        dict.put("minFurnaceTempC", Dict.MIN_FURNACE_TEMP_C);
        dict.put("minResidenceS", Dict.MIN_RESIDENCE_S);
        return dict;
    }

    // ---------------- 内部方法 ----------------

    private FurnacePayload.VO toVO(Incinerator furnace) {
        List<String> items = unqualifiedItems(furnace);
        return FurnacePayload.VO.from(furnace, items.isEmpty(), items);
    }

    private List<String> unqualifiedItems(Incinerator furnace) {
        List<String> items = new ArrayList<>();
        BigDecimal temp = furnace.getFurnaceTempC();
        BigDecimal residence = furnace.getFlueGasResidenceS();
        if (nvl(temp).compareTo(Dict.MIN_FURNACE_TEMP_C) < 0) {
            items.add("炉膛温度 " + nvl(temp).setScale(1, RoundingMode.HALF_UP) + "℃，低于 "
                    + Dict.MIN_FURNACE_TEMP_C + "℃ 达标线");
        }
        if (nvl(residence).compareTo(Dict.MIN_RESIDENCE_S) < 0) {
            items.add("烟气停留时间 " + nvl(residence).setScale(2, RoundingMode.HALF_UP) + " s，不足 "
                    + Dict.MIN_RESIDENCE_S + " s");
        }
        return items;
    }

    private Incinerator requireFurnace(String furnaceCode) {
        return incineratorRepository.findByFurnaceCode(furnaceCode)
                .orElseThrow(() -> new BizException("焚烧炉不存在：" + furnaceCode));
    }

    /** TYPE() 在不同 Hibernate 版本下可能返回 Class 或判别值，这里统一归到炉型代码 */
    private String typeCodeOf(Object raw) {
        if (raw == null) {
            return "UNKNOWN";
        }
        String text = raw instanceof Class<?> clazz ? clazz.getSimpleName() : String.valueOf(raw);
        if (text.contains("GrateFurnace") || text.equalsIgnoreCase("GRATE")) {
            return "GRATE";
        }
        if (text.contains("FluidizedBed") || text.equalsIgnoreCase("FLUIDIZED")) {
            return "FLUIDIZED";
        }
        if (text.contains("RotaryKiln") || text.equalsIgnoreCase("ROTARY")) {
            return "ROTARY";
        }
        return text;
    }

    private BigDecimal toBigDecimal(Object value) {
        return value instanceof BigDecimal decimal ? decimal : BigDecimal.valueOf(((Number) value).doubleValue());
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
