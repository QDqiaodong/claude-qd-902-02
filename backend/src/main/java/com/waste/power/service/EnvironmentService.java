package com.waste.power.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waste.power.common.BizException;
import com.waste.power.common.Dict;
import com.waste.power.domain.AshResidue;
import com.waste.power.domain.EmissionRecord;
import com.waste.power.domain.EnvAlarm;
import com.waste.power.dto.AshPayload;
import com.waste.power.dto.DashboardVO;
import com.waste.power.dto.EmissionExceedRankDTO;
import com.waste.power.dto.EmissionPayload;
import com.waste.power.repository.AshResidueRepository;
import com.waste.power.repository.EmissionRecordRepository;
import com.waste.power.repository.EnvAlarmRepository;

/**
 * 环保排放与灰渣业务：
 * - 排放指标登记：实测值超限值即判超标，自动登记环保告警，且不允许在该时段标记为「正常排放」；
 * - 灰渣与渗滤液台账登记；
 * - 仪表盘聚合结果缓存到 Redis（30 秒），写入时失效。
 */
@Service
@Transactional
public class EnvironmentService {

    private static final String DASHBOARD_CACHE_KEY = "waste-power:dashboard";

    private final EmissionRecordRepository emissionRepository;
    private final EnvAlarmRepository alarmRepository;
    private final AshResidueRepository ashRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AbnormalDisposalService disposalService;

    public EnvironmentService(EmissionRecordRepository emissionRepository, EnvAlarmRepository alarmRepository,
            AshResidueRepository ashRepository, StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
            AbnormalDisposalService disposalService) {
        this.emissionRepository = emissionRepository;
        this.alarmRepository = alarmRepository;
        this.ashRepository = ashRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.disposalService = disposalService;
    }

    @Transactional(readOnly = true)
    public List<EmissionPayload.VO> emissions() {
        return emissionRepository.findTop80ByOrderByMonitorTimeDesc().stream()
                .map(EmissionPayload.VO::from)
                .toList();
    }

    /**
     * 六种污染物各取最新一条，供半圆仪表渲染
     */
    @Transactional(readOnly = true)
    public List<EmissionPayload.VO> gauges() {
        List<EmissionPayload.VO> gauges = new ArrayList<>();
        for (String code : Dict.EMISSION_INDICATORS) {
            List<EmissionRecord> records = emissionRepository.findByIndicatorCodeOrderByMonitorTimeDesc(code);
            if (records.isEmpty()) {
                gauges.add(emptyGauge(code));
            } else {
                gauges.add(EmissionPayload.VO.from(records.get(0)));
            }
        }
        return gauges;
    }

    /**
     * 排放指标登记：超标自动登记环保告警，且不允许标记为正常排放
     */
    public EmissionPayload.VO register(EmissionPayload.Request request) {
        String code = request.getIndicatorCode();
        if (!Dict.isValidIndicator(code)) {
            throw new BizException("排放指标不合法：" + code);
        }
        BigDecimal measured = request.getMeasuredValue();
        if (measured == null || measured.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("实测值不能为空且不能为负");
        }
        BigDecimal limit = Dict.indicatorLimit(code);
        boolean exceeded = measured.compareTo(limit) > 0;
        String flag = request.getEmissionFlag() == null || request.getEmissionFlag().isBlank()
                ? (exceeded ? "EXCEED" : "NORMAL") : request.getEmissionFlag();
        if (exceeded && "NORMAL".equals(flag)) {
            throw new BizException("指标 " + Dict.indicatorName(code) + " 实测 " + measured + " "
                    + Dict.indicatorUnit(code) + " 已超限值 " + limit + " " + Dict.indicatorUnit(code)
                    + "，不允许在该时段标记为「正常排放」");
        }

        EmissionRecord record = new EmissionRecord();
        record.setIndicatorCode(code);
        record.setIndicatorName(Dict.indicatorName(code));
        record.setMeasuredValue(measured);
        record.setLimitValue(limit);
        record.setUnit(Dict.indicatorUnit(code));
        record.setExceeded(exceeded);
        record.setExceedRatio(measured.subtract(limit)
                .divide(limit, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP));
        record.setPeriodLabel(request.getPeriodLabel() == null || request.getPeriodLabel().isBlank()
                ? defaultPeriodLabel() : request.getPeriodLabel());
        record.setMonitorTime(request.getMonitorTime() == null ? LocalDateTime.now() : request.getMonitorTime());
        record.setStackCode(request.getStackCode() == null || request.getStackCode().isBlank()
                ? "STACK-01" : request.getStackCode());
        record.setEmissionFlag(flag);
        record.setRemark(request.getRemark());
        EmissionRecord saved = emissionRepository.save(record);

        if (exceeded) {
            EnvAlarm alarm = createAlarm(saved);
            // 同炉严重排放告警：排放登记事务提交后再自动建处置单（独立事务），
            // 保证告警已落库、证据外键有效；后到告警由处置服务负责合并/反弹，不依赖已处理标记
            if ("SEVERE".equals(alarm.getAlarmLevel())) {
                Long alarmId = alarm.getId();
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            disposalService.signalSevereAlarm(alarmId);
                        }
                    });
                } else {
                    disposalService.signalSevereAlarm(alarmId);
                }
            }
        }
        evictDashboard();
        return EmissionPayload.VO.from(saved);
    }

    /**
     * 自动登记环保告警
     */
    private EnvAlarm createAlarm(EmissionRecord record) {
        EnvAlarm alarm = new EnvAlarm();
        alarm.setAlarmNo("ALM" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
        alarm.setIndicatorCode(record.getIndicatorCode());
        alarm.setIndicatorName(record.getIndicatorName());
        alarm.setMeasuredValue(record.getMeasuredValue());
        alarm.setLimitValue(record.getLimitValue());
        alarm.setExceedRatio(record.getExceedRatio());
        alarm.setAlarmLevel(Dict.alarmLevel(BigDecimal.valueOf(
                record.getMeasuredValue().doubleValue() / record.getLimitValue().doubleValue())));
        alarm.setPeriodLabel(record.getPeriodLabel());
        alarm.setStackCode(record.getStackCode());
        alarm.setAlarmTime(record.getMonitorTime());
        alarm.setHandled(false);
        alarm.setHandleNote("");
        return alarmRepository.save(alarm);
    }

    @Transactional(readOnly = true)
    public List<EmissionPayload.AlarmVO> alarms() {
        return alarmRepository.findTop30ByOrderByAlarmTimeDesc().stream()
                .map(EmissionPayload.AlarmVO::from)
                .toList();
    }

    public EmissionPayload.AlarmVO handleAlarm(Long id, EmissionPayload.HandleRequest request) {
        EnvAlarm alarm = alarmRepository.findById(id)
                .orElseThrow(() -> new BizException("环保告警不存在：id=" + id));
        alarm.setHandled(true);
        alarm.setHandleNote(request.getHandleNote() == null ? "已确认并调整工况" : request.getHandleNote());
        evictDashboard();
        return EmissionPayload.AlarmVO.from(alarmRepository.save(alarm));
    }

    /**
     * 【JPQL】排放超标次数排行
     */
    @Transactional(readOnly = true)
    public List<EmissionExceedRankDTO> exceedRank() {
        List<EmissionExceedRankDTO> rank = emissionRepository.exceedRank();
        for (EmissionExceedRankDTO item : rank) {
            item.setIndicatorName(Dict.indicatorName(item.getIndicatorCode()));
        }
        return rank;
    }

    /**
     * 仪表盘聚合（Redis 缓存 30 秒）
     */
    @Transactional(readOnly = true)
    public DashboardVO dashboard() {
        String cached = redisTemplate.opsForValue().get(DASHBOARD_CACHE_KEY);
        if (cached != null && !cached.isBlank()) {
            try {
                DashboardVO vo = objectMapper.readValue(cached, DashboardVO.class);
                vo.setCached(true);
                return vo;
            } catch (Exception ignored) {
                // 缓存解析失败则回源
            }
        }
        DashboardVO vo = buildDashboard();
        vo.setCached(false);
        try {
            redisTemplate.opsForValue().set(DASHBOARD_CACHE_KEY, objectMapper.writeValueAsString(vo),
                    Duration.ofSeconds(30));
        } catch (Exception ignored) {
            // 缓存写入失败不影响主流程
        }
        return vo;
    }

    /**
     * 灰渣与渗滤液台账
     */
    @Transactional(readOnly = true)
    public List<AshPayload.VO> ashRecords() {
        return ashRepository.findAll().stream()
                .sorted((a, b) -> b.getRecordDate().compareTo(a.getRecordDate()))
                .limit(15)
                .map(AshPayload.VO::from)
                .toList();
    }

    /**
     * 登记当日灰渣与渗滤液产出
     */
    public AshPayload.VO registerAsh(AshPayload.Request request) {
        LocalDate date = request.getRecordDate() == null ? LocalDate.now() : request.getRecordDate();
        BigDecimal flyAsh = nvl(request.getFlyAshTon());
        BigDecimal bottomAsh = nvl(request.getBottomAshTon());
        BigDecimal solidified = request.getSolidifiedTon();
        if (flyAsh.compareTo(BigDecimal.ZERO) <= 0 && bottomAsh.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("飞灰与炉渣至少登记一项产出量");
        }
        if (solidified != null && solidified.compareTo(flyAsh) > 0) {
            throw new BizException("固化量 " + solidified + " t 不得大于飞灰产生量 " + flyAsh + " t");
        }
        if (request.getDisposalDest() != null && !Dict.ASH_DISPOSAL.contains(request.getDisposalDest())) {
            throw new BizException("固化后去向不在可选范围：" + request.getDisposalDest());
        }
        if (request.getLeachateTreatedM3() != null && request.getLeachateTreatedM3().compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("渗滤液处理量不能为负");
        }

        AshResidue record = new AshResidue();
        record.setRecordDate(date);
        record.setFlyAshTon(flyAsh);
        record.setBottomAshTon(bottomAsh);
        record.setSolidifiedTon(solidified == null ? BigDecimal.ZERO : solidified);
        record.setDisposalDest(request.getDisposalDest() == null ? Dict.ASH_DISPOSAL.get(0) : request.getDisposalDest());
        record.setLeachateTreatedM3(nvl(request.getLeachateTreatedM3()));
        record.setEffluentCod(request.getEffluentCod());
        record.setEffluentOk(request.getEffluentCod() != null
                && AshPayload.VO.effluentQualified(request.getEffluentCod()));
        record.setShift(request.getShift() == null || request.getShift().isBlank() ? "白班" : request.getShift());
        record.setRemark(request.getRemark());
        evictDashboard();
        return AshPayload.VO.from(ashRepository.save(record));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dict() {
        Map<String, Object> dict = new LinkedHashMap<>();
        List<Map<String, Object>> indicators = new ArrayList<>();
        for (String code : Dict.EMISSION_INDICATORS) {
            indicators.add(Map.of("code", code, "name", Dict.indicatorName(code),
                    "unit", Dict.indicatorUnit(code), "limit", Dict.indicatorLimit(code)));
        }
        dict.put("indicators", indicators);
        dict.put("emissionFlags", Dict.EMISSION_FLAGS.stream()
                .map(code -> (Object) Map.of("code", code, "name", Dict.emissionFlagName(code)))
                .toList());
        dict.put("alarmLevels", Dict.ALARM_LEVELS.stream()
                .map(code -> (Object) Map.of("code", code, "name", Dict.alarmLevelName(code)))
                .toList());
        dict.put("ashDisposal", Dict.ASH_DISPOSAL);
        dict.put("effluentCodLimit", Dict.EFFLUENT_COD_LIMIT);
        return dict;
    }

    // ---------------- 内部方法 ----------------

    private DashboardVO buildDashboard() {
        DashboardVO vo = new DashboardVO();
        List<EmissionPayload.VO> gauges = gauges();
        vo.setGauges(gauges);
        vo.setAlarms(alarmRepository.findTop20ByHandledFalseOrderByAlarmTimeDesc().stream()
                .map(EmissionPayload.AlarmVO::from).toList());
        vo.setAshRecords(ashRecords());
        vo.setRank(exceedRank());
        vo.setExceedCount(gauges.stream().filter(item -> Boolean.TRUE.equals(item.getExceeded())).count());
        vo.setAlarmCount(alarmRepository.count());
        BigDecimal flyAsh = BigDecimal.ZERO;
        BigDecimal bottomAsh = BigDecimal.ZERO;
        BigDecimal leachate = BigDecimal.ZERO;
        for (AshPayload.VO ash : vo.getAshRecords()) {
            flyAsh = flyAsh.add(nvl(ash.getFlyAshTon()));
            bottomAsh = bottomAsh.add(nvl(ash.getBottomAshTon()));
            leachate = leachate.add(nvl(ash.getLeachateTreatedM3()));
        }
        vo.setFlyAshTotalTon(flyAsh.setScale(2, RoundingMode.HALF_UP));
        vo.setBottomAshTotalTon(bottomAsh.setScale(2, RoundingMode.HALF_UP));
        vo.setLeachateTotalM3(leachate.setScale(2, RoundingMode.HALF_UP));
        return vo;
    }

    private EmissionPayload.VO emptyGauge(String code) {
        EmissionPayload.VO vo = new EmissionPayload.VO();
        vo.setIndicatorCode(code);
        vo.setIndicatorName(Dict.indicatorName(code));
        vo.setUnit(Dict.indicatorUnit(code));
        vo.setLimitValue(Dict.indicatorLimit(code));
        vo.setMeasuredValue(BigDecimal.ZERO);
        vo.setExceeded(false);
        vo.setExceedRatio(BigDecimal.ZERO);
        vo.setGaugeRatio(0D);
        vo.setEmissionFlag("NORMAL");
        vo.setEmissionFlagName(Dict.emissionFlagName("NORMAL"));
        return vo;
    }

    private String defaultPeriodLabel() {
        LocalDateTime now = LocalDateTime.now();
        return now.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")) + " 时段";
    }

    private void evictDashboard() {
        try {
            redisTemplate.delete(DASHBOARD_CACHE_KEY);
        } catch (Exception ignored) {
            // Redis 不可用时忽略
        }
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
