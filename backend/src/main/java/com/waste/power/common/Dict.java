package com.waste.power.common;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 生活垃圾焚烧发电厂业务词典与规则参数表：
 * 垃圾类别、来源行政区划与中转站、地磅、炉型（炉排炉/流化床/回转窑）、燃烧状态、
 * 启停炉动作、排放指标与国标限值、环保告警等级、锅炉与机组状态、灰渣去向等。
 *
 * 所有业务阈值集中在此，便于统一调整。
 */
public final class Dict {

    private Dict() {
    }

    // ---------------- 垃圾类别 ----------------
    public static final List<String> WASTE_CATEGORIES = List.of("KITCHEN", "OTHER", "RECYCLABLE", "HAZARDOUS");

    private static final Map<String, String> WASTE_CATEGORY_NAME = Map.of(
            "KITCHEN", "厨余垃圾",
            "OTHER", "其他垃圾",
            "RECYCLABLE", "可回收物",
            "HAZARDOUS", "有害垃圾");

    /** 各类垃圾入炉参考低位热值 kJ/kg */
    private static final Map<String, BigDecimal> WASTE_CALORIFIC = Map.of(
            "KITCHEN", bd(4200),
            "OTHER", bd(7500),
            "RECYCLABLE", bd(9200),
            "HAZARDOUS", bd(2800));

    /** 不宜直接进入焚烧炉的类别（须先分拣/暂存） */
    private static final Set<String> RESTRICTED_CATEGORIES = Set.of("HAZARDOUS");

    // ---------------- 来源行政区划与中转站 ----------------
    public static final List<String> SOURCE_REGIONS = List.of(
            "江城区", "阳东区", "阳西县", "海陵区", "高新区");

    private static final Map<String, List<String>> REGION_STATIONS = Map.of(
            "江城区", List.of("城东中转站", "城南中转站", "城北压缩站"),
            "阳东区", List.of("阳东压缩站", "东平中转站"),
            "阳西县", List.of("阳西中转站", "织篢中转站"),
            "海陵区", List.of("海陵中转站"),
            "高新区", List.of("高新中转站", "港口压缩站"));

    /** 进厂地磅编号 */
    public static final List<String> SCALE_NOS = List.of("SCALE-01", "SCALE-02", "SCALE-03");

    /** 储坑抓斗编号 */
    public static final List<String> GRAB_NOS = List.of("GRAB-01", "GRAB-02", "GRAB-03", "GRAB-04");

    // ---------------- 炉型 ----------------
    public static final List<String> FURNACE_TYPES = List.of("GRATE", "FLUIDIZED", "ROTARY");

    private static final Map<String, String> FURNACE_TYPE_NAME = Map.of(
            "GRATE", "炉排炉",
            "FLUIDIZED", "流化床",
            "ROTARY", "回转窑");

    /** 各炉型的典型额定处理量区间 t/d */
    private static final Map<String, String> FURNACE_TYPE_RANGE = Map.of(
            "GRATE", "500 ~ 1200 t/d",
            "FLUIDIZED", "300 ~ 600 t/d",
            "ROTARY", "100 ~ 350 t/d");

    // ---------------- 燃烧状态 ----------------
    public static final List<String> COMBUSTION_STATUS = List.of(
            "STARTING", "STABLE", "BANKING", "SHUTDOWN", "FAULT");

    private static final Map<String, String> COMBUSTION_STATUS_NAME = Map.of(
            "STARTING", "启炉中",
            "STABLE", "稳燃",
            "BANKING", "压火",
            "SHUTDOWN", "停炉",
            "FAULT", "工况异常");

    /** 燃烧状态推进顺序（启炉 → 稳燃 → 停炉） */
    public static final List<String> COMBUSTION_FLOW = List.of("STARTING", "STABLE", "SHUTDOWN");

    /** 启停炉动作 */
    public static final List<String> FURNACE_ACTIONS = List.of("START", "STABILIZE", "STOP");

    private static final Map<String, String> FURNACE_ACTION_NAME = Map.of(
            "START", "启炉",
            "STABILIZE", "稳燃",
            "STOP", "停炉");

    // ---------------- 储坑状态 ----------------
    public static final List<String> PIT_STATUS = List.of("FERMENTING", "NORMAL", "FULL", "OVERHAUL");

    private static final Map<String, String> PIT_STATUS_NAME = Map.of(
            "FERMENTING", "发酵中",
            "NORMAL", "正常",
            "FULL", "满仓",
            "OVERHAUL", "清空检修");

    // ---------------- 锅炉 / 汽轮发电机状态 ----------------
    public static final List<String> BOILER_STATUS = List.of("RUNNING", "BANKING", "OVERHAUL");

    private static final Map<String, String> BOILER_STATUS_NAME = Map.of(
            "RUNNING", "运行",
            "BANKING", "热备",
            "OVERHAUL", "检修");

    public static final List<String> UNIT_STATUS = List.of("ON_GRID", "IDLE", "OVERHAUL");

    private static final Map<String, String> UNIT_STATUS_NAME = Map.of(
            "ON_GRID", "并网发电",
            "IDLE", "热备停机",
            "OVERHAUL", "检修");

    // ---------------- 发电日结（相邻表码增量口径） ----------------
    /** 日结状态：已日结 / 暂停待校表 / 已校表待日结 */
    public static final List<String> SETTLEMENT_STATUS = List.of("SETTLED", "SUSPENDED", "VERIFIED");

    private static final Map<String, String> SETTLEMENT_STATUS_NAME = Map.of(
            "SETTLED", "已日结",
            "SUSPENDED", "暂停待校表",
            "VERIFIED", "已校表待日结");

    /** 当日增量的基数来源 */
    public static final List<String> BASIS_SOURCES = List.of(
            "PREV_DAY", "INIT_BASE", "NEW_METER_BASE", "VERIFIED_BASE");

    private static final Map<String, String> BASIS_SOURCE_NAME = Map.of(
            "PREV_DAY", "上日日结表码",
            "INIT_BASE", "首次建档基数",
            "NEW_METER_BASE", "换表启用新基数",
            "VERIFIED_BASE", "校表确认基数");

    /** 表计事件 */
    public static final List<String> METER_EVENTS = List.of("NONE", "ROLLBACK", "METER_REPLACED");

    private static final Map<String, String> METER_EVENT_NAME = Map.of(
            "NONE", "无异常",
            "ROLLBACK", "表计回拨",
            "METER_REPLACED", "更换表计");

    /** 日结异常状态 */
    public static final List<String> METER_ANOMALIES = List.of(
            "NORMAL", "METER_ROLLBACK", "METER_REPLACED", "SUSPENDED");

    private static final Map<String, String> METER_ANOMALY_NAME = Map.of(
            "NORMAL", "正常",
            "METER_ROLLBACK", "表计回拨",
            "METER_REPLACED", "更换表计",
            "SUSPENDED", "暂停日结");

    /** 表计回拨/更换时的明确处置选择 */
    public static final List<String> METER_DECISIONS = List.of("SEAL_NEW_BASE", "SUSPEND");

    private static final Map<String, String> METER_DECISION_NAME = Map.of(
            "SEAL_NEW_BASE", "旧表封账并启用新基数",
            "SUSPEND", "暂停日结等待校表");

    /** 上网电量缺省比例（未抄上网关口表时按发电量 85% 估算） */
    public static final BigDecimal DEFAULT_GRID_RATE = bd(0.85);

    // ---------------- 排放指标与限值（GB 18485-2014 日均值口径） ----------------
    public static final List<String> EMISSION_INDICATORS = List.of(
            "PARTICULATE", "SO2", "NOX", "HCL", "CO", "DIOXIN");

    private static final Map<String, String> INDICATOR_NAME = Map.of(
            "PARTICULATE", "颗粒物",
            "SO2", "二氧化硫 SO₂",
            "NOX", "氮氧化物 NOx",
            "HCL", "氯化氢 HCl",
            "CO", "一氧化碳 CO",
            "DIOXIN", "二噁英");

    private static final Map<String, String> INDICATOR_UNIT = Map.of(
            "PARTICULATE", "mg/m³",
            "SO2", "mg/m³",
            "NOX", "mg/m³",
            "HCL", "mg/m³",
            "CO", "mg/m³",
            "DIOXIN", "ng-TEQ/m³");

    /** 国标排放限值（日均值） */
    private static final Map<String, BigDecimal> INDICATOR_LIMIT = Map.of(
            "PARTICULATE", bd(20),
            "SO2", bd(80),
            "NOX", bd(250),
            "HCL", bd(50),
            "CO", bd(80),
            "DIOXIN", bd(0.10));

    /** 排放标记 */
    public static final List<String> EMISSION_FLAGS = List.of("NORMAL", "EXCEED");

    private static final Map<String, String> EMISSION_FLAG_NAME = Map.of(
            "NORMAL", "正常排放",
            "EXCEED", "超标排放");

    // ---------------- 环保告警等级 ----------------
    public static final List<String> ALARM_LEVELS = List.of("MINOR", "MAJOR", "SEVERE");

    private static final Map<String, String> ALARM_LEVEL_NAME = Map.of(
            "MINOR", "一般",
            "MAJOR", "较重",
            "SEVERE", "严重");

    // ---------------- 灰渣去向 ----------------
    public static final List<String> ASH_DISPOSAL = List.of(
            "填埋场专区填埋", "水泥窑协同处置", "危废刚性填埋场", "综合利用制砖");

    // ---------------- 异常工况处置单 ----------------
    /** 处置单状态：待处置 → 处置中 → 待复核 → 已恢复 → 已关闭 */
    public static final List<String> DISPOSAL_STATUS = List.of(
            "PENDING", "HANDLING", "REVIEWING", "RECOVERED", "CLOSED");

    private static final Map<String, String> DISPOSAL_STATUS_NAME = Map.of(
            "PENDING", "待处置",
            "HANDLING", "处置中",
            "REVIEWING", "待复核",
            "RECOVERED", "已恢复",
            "CLOSED", "已关闭");

    /** 触发来源 */
    public static final List<String> DISPOSAL_SOURCES = List.of("AUTO", "MANUAL");

    private static final Map<String, String> DISPOSAL_SOURCE_NAME = Map.of(
            "AUTO", "自动触发",
            "MANUAL", "人工发起");

    /** 告警数据缺失时的明确选择 */
    public static final List<String> MISSING_CHOICES = List.of("WAIT_FREEZE", "CLOSE_KEEP_FROZEN");

    private static final Map<String, String> MISSING_CHOICE_NAME = Map.of(
            "WAIT_FREEZE", "继续冻结等待补齐",
            "CLOSE_KEEP_FROZEN", "记录理由后关闭处置但保持投料冻结");

    /** 证据挂接来源 */
    private static final Map<String, String> EVIDENCE_BIND_NAME = Map.of(
            "SNAPSHOT", "建单快照",
            "MERGED", "并发合并");

    /** 严重告警参与处置联锁的回看窗口 h（只统计冻结时点之后且窗口内的未解除严重告警） */
    public static final long SEVERE_ALARM_LOOKBACK_HOURS = 24;

    // ---------------- 业务规则阈值 ----------------
    /** 炉膛温度达标下限 ℃（GB 18485：不低于 850℃） */
    public static final BigDecimal MIN_FURNACE_TEMP_C = bd(850);

    /** 烟气在 850℃ 以上停留时间下限 s（不低于 2 秒） */
    public static final BigDecimal MIN_RESIDENCE_S = bd(2.0);

    /** 焚烧炉炉膛 CO 浓度预警值 ppm */
    public static final BigDecimal CO_ALARM_PPM = bd(150);

    /** 单车净重上限 t（地磅量程与运输车载重约束） */
    public static final BigDecimal MAX_TRUCK_NET_TON = bd(45);

    /** 判定为「接近满仓」的储坑库存率 */
    public static final BigDecimal PIT_FULL_RATE = bd(0.90);

    /** 渗滤液处理出水 COD 限值 mg/L */
    public static final BigDecimal EFFLUENT_COD_LIMIT = bd(100);

    /** 厂用电率上限 % */
    public static final BigDecimal MAX_AUXILIARY_RATE = bd(20);

    /** 吨垃圾折算发电量 kWh/t（按入炉量估算当班发电量） */
    public static final BigDecimal POWER_PER_TON_KWH = bd(320);

    private static BigDecimal bd(double value) {
        return BigDecimal.valueOf(value);
    }

    // ---------------- 取值方法 ----------------

    public static boolean isValidCategory(String category) {
        return category != null && WASTE_CATEGORY_NAME.containsKey(category);
    }

    public static String categoryName(String category) {
        return category == null ? "" : WASTE_CATEGORY_NAME.getOrDefault(category, category);
    }

    public static BigDecimal categoryCalorific(String category) {
        return WASTE_CALORIFIC.getOrDefault(category, bd(6000));
    }

    public static boolean isRestricted(String category) {
        return category != null && RESTRICTED_CATEGORIES.contains(category);
    }

    public static boolean isValidRegion(String region) {
        return region != null && SOURCE_REGIONS.contains(region);
    }

    public static List<String> stations(String region) {
        return REGION_STATIONS.getOrDefault(region, List.of());
    }

    public static boolean isValidStation(String region, String station) {
        return station != null && stations(region).contains(station);
    }

    public static boolean isValidFurnaceType(String type) {
        return type != null && FURNACE_TYPE_NAME.containsKey(type);
    }

    public static String furnaceTypeName(String type) {
        return type == null ? "" : FURNACE_TYPE_NAME.getOrDefault(type, type);
    }

    public static String furnaceTypeRange(String type) {
        return type == null ? "" : FURNACE_TYPE_RANGE.getOrDefault(type, "");
    }

    public static boolean isValidCombustionStatus(String status) {
        return status != null && COMBUSTION_STATUS_NAME.containsKey(status);
    }

    public static String combustionStatusName(String status) {
        return status == null ? "" : COMBUSTION_STATUS_NAME.getOrDefault(status, status);
    }

    public static boolean isValidFurnaceAction(String action) {
        return action != null && FURNACE_ACTION_NAME.containsKey(action);
    }

    public static String furnaceActionName(String action) {
        return action == null ? "" : FURNACE_ACTION_NAME.getOrDefault(action, action);
    }

    public static boolean isValidPitStatus(String status) {
        return status != null && PIT_STATUS_NAME.containsKey(status);
    }

    public static String pitStatusName(String status) {
        return status == null ? "" : PIT_STATUS_NAME.getOrDefault(status, status);
    }

    public static String boilerStatusName(String status) {
        return status == null ? "" : BOILER_STATUS_NAME.getOrDefault(status, status);
    }

    public static String unitStatusName(String status) {
        return status == null ? "" : UNIT_STATUS_NAME.getOrDefault(status, status);
    }

    public static String settlementStatusName(String status) {
        return status == null ? "" : SETTLEMENT_STATUS_NAME.getOrDefault(status, status);
    }

    public static String basisSourceName(String code) {
        return code == null ? "" : BASIS_SOURCE_NAME.getOrDefault(code, code);
    }

    public static String meterEventName(String code) {
        return code == null ? "" : METER_EVENT_NAME.getOrDefault(code, code);
    }

    public static String meterAnomalyName(String code) {
        return code == null ? "" : METER_ANOMALY_NAME.getOrDefault(code, code);
    }

    public static String meterDecisionName(String code) {
        return code == null ? "" : METER_DECISION_NAME.getOrDefault(code, code);
    }

    public static boolean isValidIndicator(String indicator) {
        return indicator != null && INDICATOR_NAME.containsKey(indicator);
    }

    public static String indicatorName(String indicator) {
        return indicator == null ? "" : INDICATOR_NAME.getOrDefault(indicator, indicator);
    }

    public static String indicatorUnit(String indicator) {
        return indicator == null ? "" : INDICATOR_UNIT.getOrDefault(indicator, "");
    }

    public static BigDecimal indicatorLimit(String indicator) {
        return INDICATOR_LIMIT.getOrDefault(indicator, bd(100));
    }

    public static String emissionFlagName(String flag) {
        return flag == null ? "" : EMISSION_FLAG_NAME.getOrDefault(flag, flag);
    }

    public static String alarmLevelName(String level) {
        return level == null ? "" : ALARM_LEVEL_NAME.getOrDefault(level, level);
    }

    public static String disposalStatusName(String status) {
        return status == null ? "" : DISPOSAL_STATUS_NAME.getOrDefault(status, status);
    }

    public static String disposalSourceName(String source) {
        return source == null ? "" : DISPOSAL_SOURCE_NAME.getOrDefault(source, source);
    }

    public static String missingChoiceName(String choice) {
        return choice == null ? "" : MISSING_CHOICE_NAME.getOrDefault(choice, choice);
    }

    public static String evidenceBindName(String bind) {
        return bind == null ? "" : EVIDENCE_BIND_NAME.getOrDefault(bind, bind);
    }

    /** 按超标倍率给出告警等级：≥2 倍严重，≥1.2 倍较重，其余一般 */
    public static String alarmLevel(BigDecimal ratio) {
        if (ratio == null) {
            return "MINOR";
        }
        if (ratio.compareTo(bd(2.0)) >= 0) {
            return "SEVERE";
        }
        if (ratio.compareTo(bd(1.2)) >= 0) {
            return "MAJOR";
        }
        return "MINOR";
    }

    /** 全部排放指标的限值表（有序，便于页面按固定顺序展示仪表） */
    public static Map<String, BigDecimal> indicatorLimits() {
        Map<String, BigDecimal> limits = new LinkedHashMap<>();
        for (String indicator : EMISSION_INDICATORS) {
            limits.put(indicator, INDICATOR_LIMIT.get(indicator));
        }
        return limits;
    }
}
