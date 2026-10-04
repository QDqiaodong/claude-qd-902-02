package com.waste.power.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.waste.power.common.Dict;
import com.waste.power.domain.FluidizedBed;
import com.waste.power.domain.FurnaceRunLog;
import com.waste.power.domain.GrateFurnace;
import com.waste.power.domain.Incinerator;
import com.waste.power.domain.RotaryKiln;

/**
 * 焚烧炉模块的请求与视图对象。
 * VO 按子类（炉排炉/流化床/回转窑）携带各自的专属参数，前端据此按炉型渲染不同参数。
 */
public class FurnacePayload {

    /** 炉型参数视图（子类专属字段按炉型填充，其余为 null） */
    public static class VO {

        private Long id;
        private String furnaceCode;
        private String furnaceName;
        /** 子类标识：GRATE / FLUIDIZED / ROTARY */
        private String furnaceType;
        private String furnaceTypeName;
        private BigDecimal ratedCapacityTpd;
        private BigDecimal furnaceTempC;
        private BigDecimal flueGasResidenceS;
        private BigDecimal coPpm;
        private String combustionStatus;
        private String combustionStatusName;
        private BigDecimal dailyProcessTon;
        private BigDecimal runningHours;
        private LocalDateTime lastStartTime;
        private String grabNo;
        /** 对应排放口（同炉严重告警归集依据） */
        private String stackCode;
        private String remark;

        // 炉排炉专属
        private BigDecimal grateSpeedMh;
        private BigDecimal grateBedThicknessMm;
        private Integer grateStageCount;
        private BigDecimal primaryAirTempC;

        // 流化床专属
        private BigDecimal bedMaterialHeightMm;
        private BigDecimal fluidizingAirSpeedMs;
        private BigDecimal bedTempC;
        private BigDecimal bedMaterialSizeMm;

        // 回转窑专属
        private BigDecimal kilnSpeedRpm;
        private BigDecimal kilnInclinationDeg;
        private BigDecimal kilnLengthM;
        private BigDecimal kilnTailTempC;

        /** 炉膛温度是否达标（≥850℃ 且停留 ≥2s） */
        private Boolean combustionQualified;
        /** 不达标项明细 */
        private List<String> unqualifiedItems;
        /** 按炉型给出的专属参数键值对（前端直接遍历渲染） */
        private Map<String, Object> extraParams;

        public static VO from(Incinerator furnace, boolean qualified, List<String> unqualifiedItems) {
            VO vo = new VO();
            vo.id = furnace.getId();
            vo.furnaceCode = furnace.getFurnaceCode();
            vo.furnaceName = furnace.getFurnaceName();
            vo.furnaceType = furnace.getFurnaceTypeCode();
            vo.furnaceTypeName = Dict.furnaceTypeName(furnace.getFurnaceTypeCode());
            vo.ratedCapacityTpd = furnace.getRatedCapacityTpd();
            vo.furnaceTempC = furnace.getFurnaceTempC();
            vo.flueGasResidenceS = furnace.getFlueGasResidenceS();
            vo.coPpm = furnace.getCoPpm();
            vo.combustionStatus = furnace.getCombustionStatus();
            vo.combustionStatusName = Dict.combustionStatusName(furnace.getCombustionStatus());
            vo.dailyProcessTon = furnace.getDailyProcessTon();
            vo.runningHours = furnace.getRunningHours();
            vo.lastStartTime = furnace.getLastStartTime();
            vo.grabNo = furnace.getGrabNo();
            vo.stackCode = furnace.getStackCode();
            vo.remark = furnace.getRemark();
            vo.combustionQualified = qualified;
            vo.unqualifiedItems = unqualifiedItems;
            vo.extraParams = new LinkedHashMap<>();

            if (furnace instanceof GrateFurnace grate) {
                vo.grateSpeedMh = grate.getGrateSpeedMh();
                vo.grateBedThicknessMm = grate.getGrateBedThicknessMm();
                vo.grateStageCount = grate.getGrateStageCount();
                vo.primaryAirTempC = grate.getPrimaryAirTempC();
                vo.extraParams.put("炉排速度", value(grate.getGrateSpeedMh(), "m/h"));
                vo.extraParams.put("料层厚度", value(grate.getGrateBedThicknessMm(), "mm"));
                vo.extraParams.put("炉排段数", grate.getGrateStageCount() == null ? "--" : grate.getGrateStageCount() + " 段");
                vo.extraParams.put("一次风温", value(grate.getPrimaryAirTempC(), "℃"));
            } else if (furnace instanceof FluidizedBed bed) {
                vo.bedMaterialHeightMm = bed.getBedMaterialHeightMm();
                vo.fluidizingAirSpeedMs = bed.getFluidizingAirSpeedMs();
                vo.bedTempC = bed.getBedTempC();
                vo.bedMaterialSizeMm = bed.getBedMaterialSizeMm();
                vo.extraParams.put("床料高度", value(bed.getBedMaterialHeightMm(), "mm"));
                vo.extraParams.put("流化风速", value(bed.getFluidizingAirSpeedMs(), "m/s"));
                vo.extraParams.put("床温", value(bed.getBedTempC(), "℃"));
                vo.extraParams.put("床料粒径", value(bed.getBedMaterialSizeMm(), "mm"));
            } else if (furnace instanceof RotaryKiln kiln) {
                vo.kilnSpeedRpm = kiln.getKilnSpeedRpm();
                vo.kilnInclinationDeg = kiln.getKilnInclinationDeg();
                vo.kilnLengthM = kiln.getKilnLengthM();
                vo.kilnTailTempC = kiln.getKilnTailTempC();
                vo.extraParams.put("窑体转速", value(kiln.getKilnSpeedRpm(), "r/min"));
                vo.extraParams.put("窑体倾角", value(kiln.getKilnInclinationDeg(), "°"));
                vo.extraParams.put("窑长", value(kiln.getKilnLengthM(), "m"));
                vo.extraParams.put("窑尾温度", value(kiln.getKilnTailTempC(), "℃"));
            }
            return vo;
        }

        private static String value(BigDecimal number, String unit) {
            return number == null ? "--" : number.stripTrailingZeros().toPlainString() + " " + unit;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getFurnaceCode() {
            return furnaceCode;
        }

        public void setFurnaceCode(String furnaceCode) {
            this.furnaceCode = furnaceCode;
        }

        public String getFurnaceName() {
            return furnaceName;
        }

        public void setFurnaceName(String furnaceName) {
            this.furnaceName = furnaceName;
        }

        public String getFurnaceType() {
            return furnaceType;
        }

        public void setFurnaceType(String furnaceType) {
            this.furnaceType = furnaceType;
        }

        public String getFurnaceTypeName() {
            return furnaceTypeName;
        }

        public void setFurnaceTypeName(String furnaceTypeName) {
            this.furnaceTypeName = furnaceTypeName;
        }

        public BigDecimal getRatedCapacityTpd() {
            return ratedCapacityTpd;
        }

        public void setRatedCapacityTpd(BigDecimal ratedCapacityTpd) {
            this.ratedCapacityTpd = ratedCapacityTpd;
        }

        public BigDecimal getFurnaceTempC() {
            return furnaceTempC;
        }

        public void setFurnaceTempC(BigDecimal furnaceTempC) {
            this.furnaceTempC = furnaceTempC;
        }

        public BigDecimal getFlueGasResidenceS() {
            return flueGasResidenceS;
        }

        public void setFlueGasResidenceS(BigDecimal flueGasResidenceS) {
            this.flueGasResidenceS = flueGasResidenceS;
        }

        public BigDecimal getCoPpm() {
            return coPpm;
        }

        public void setCoPpm(BigDecimal coPpm) {
            this.coPpm = coPpm;
        }

        public String getCombustionStatus() {
            return combustionStatus;
        }

        public void setCombustionStatus(String combustionStatus) {
            this.combustionStatus = combustionStatus;
        }

        public String getCombustionStatusName() {
            return combustionStatusName;
        }

        public void setCombustionStatusName(String combustionStatusName) {
            this.combustionStatusName = combustionStatusName;
        }

        public BigDecimal getDailyProcessTon() {
            return dailyProcessTon;
        }

        public void setDailyProcessTon(BigDecimal dailyProcessTon) {
            this.dailyProcessTon = dailyProcessTon;
        }

        public BigDecimal getRunningHours() {
            return runningHours;
        }

        public void setRunningHours(BigDecimal runningHours) {
            this.runningHours = runningHours;
        }

        public LocalDateTime getLastStartTime() {
            return lastStartTime;
        }

        public void setLastStartTime(LocalDateTime lastStartTime) {
            this.lastStartTime = lastStartTime;
        }

        public String getGrabNo() {
            return grabNo;
        }

        public void setGrabNo(String grabNo) {
            this.grabNo = grabNo;
        }

        public String getStackCode() {
            return stackCode;
        }

        public void setStackCode(String stackCode) {
            this.stackCode = stackCode;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public BigDecimal getGrateSpeedMh() {
            return grateSpeedMh;
        }

        public void setGrateSpeedMh(BigDecimal grateSpeedMh) {
            this.grateSpeedMh = grateSpeedMh;
        }

        public BigDecimal getGrateBedThicknessMm() {
            return grateBedThicknessMm;
        }

        public void setGrateBedThicknessMm(BigDecimal grateBedThicknessMm) {
            this.grateBedThicknessMm = grateBedThicknessMm;
        }

        public Integer getGrateStageCount() {
            return grateStageCount;
        }

        public void setGrateStageCount(Integer grateStageCount) {
            this.grateStageCount = grateStageCount;
        }

        public BigDecimal getPrimaryAirTempC() {
            return primaryAirTempC;
        }

        public void setPrimaryAirTempC(BigDecimal primaryAirTempC) {
            this.primaryAirTempC = primaryAirTempC;
        }

        public BigDecimal getBedMaterialHeightMm() {
            return bedMaterialHeightMm;
        }

        public void setBedMaterialHeightMm(BigDecimal bedMaterialHeightMm) {
            this.bedMaterialHeightMm = bedMaterialHeightMm;
        }

        public BigDecimal getFluidizingAirSpeedMs() {
            return fluidizingAirSpeedMs;
        }

        public void setFluidizingAirSpeedMs(BigDecimal fluidizingAirSpeedMs) {
            this.fluidizingAirSpeedMs = fluidizingAirSpeedMs;
        }

        public BigDecimal getBedTempC() {
            return bedTempC;
        }

        public void setBedTempC(BigDecimal bedTempC) {
            this.bedTempC = bedTempC;
        }

        public BigDecimal getBedMaterialSizeMm() {
            return bedMaterialSizeMm;
        }

        public void setBedMaterialSizeMm(BigDecimal bedMaterialSizeMm) {
            this.bedMaterialSizeMm = bedMaterialSizeMm;
        }

        public BigDecimal getKilnSpeedRpm() {
            return kilnSpeedRpm;
        }

        public void setKilnSpeedRpm(BigDecimal kilnSpeedRpm) {
            this.kilnSpeedRpm = kilnSpeedRpm;
        }

        public BigDecimal getKilnInclinationDeg() {
            return kilnInclinationDeg;
        }

        public void setKilnInclinationDeg(BigDecimal kilnInclinationDeg) {
            this.kilnInclinationDeg = kilnInclinationDeg;
        }

        public BigDecimal getKilnLengthM() {
            return kilnLengthM;
        }

        public void setKilnLengthM(BigDecimal kilnLengthM) {
            this.kilnLengthM = kilnLengthM;
        }

        public BigDecimal getKilnTailTempC() {
            return kilnTailTempC;
        }

        public void setKilnTailTempC(BigDecimal kilnTailTempC) {
            this.kilnTailTempC = kilnTailTempC;
        }

        public Boolean getCombustionQualified() {
            return combustionQualified;
        }

        public void setCombustionQualified(Boolean combustionQualified) {
            this.combustionQualified = combustionQualified;
        }

        public List<String> getUnqualifiedItems() {
            return unqualifiedItems;
        }

        public void setUnqualifiedItems(List<String> unqualifiedItems) {
            this.unqualifiedItems = unqualifiedItems;
        }

        public Map<String, Object> getExtraParams() {
            return extraParams;
        }

        public void setExtraParams(Map<String, Object> extraParams) {
            this.extraParams = extraParams;
        }
    }

    /** 启炉 / 停炉 / 稳燃 推进请求 */
    public static class RunRequest {

        private String action;
        private BigDecimal furnaceTempC;
        private BigDecimal flueGasResidenceS;
        private BigDecimal processTon;
        private BigDecimal runHours;
        private String operator;
        private String remark;

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public BigDecimal getFurnaceTempC() {
            return furnaceTempC;
        }

        public void setFurnaceTempC(BigDecimal furnaceTempC) {
            this.furnaceTempC = furnaceTempC;
        }

        public BigDecimal getFlueGasResidenceS() {
            return flueGasResidenceS;
        }

        public void setFlueGasResidenceS(BigDecimal flueGasResidenceS) {
            this.flueGasResidenceS = flueGasResidenceS;
        }

        public BigDecimal getProcessTon() {
            return processTon;
        }

        public void setProcessTon(BigDecimal processTon) {
            this.processTon = processTon;
        }

        public BigDecimal getRunHours() {
            return runHours;
        }

        public void setRunHours(BigDecimal runHours) {
            this.runHours = runHours;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }

    /** 启停炉记录视图 */
    public static class LogVO {

        private Long id;
        private String furnaceCode;
        private String action;
        private String actionName;
        private LocalDateTime operateTime;
        private String periodMonth;
        private BigDecimal furnaceTempC;
        private BigDecimal processTon;
        private BigDecimal powerKwh;
        private BigDecimal runHours;
        private String operator;
        private String remark;

        public static LogVO from(FurnaceRunLog log) {
            LogVO vo = new LogVO();
            vo.id = log.getId();
            vo.furnaceCode = log.getFurnaceCode();
            vo.action = log.getAction();
            vo.actionName = Dict.furnaceActionName(log.getAction());
            vo.operateTime = log.getOperateTime();
            vo.periodMonth = log.getPeriodMonth();
            vo.furnaceTempC = log.getFurnaceTempC();
            vo.processTon = log.getProcessTon();
            vo.powerKwh = log.getPowerKwh();
            vo.runHours = log.getRunHours();
            vo.operator = log.getOperator();
            vo.remark = log.getRemark();
            return vo;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getFurnaceCode() {
            return furnaceCode;
        }

        public void setFurnaceCode(String furnaceCode) {
            this.furnaceCode = furnaceCode;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getActionName() {
            return actionName;
        }

        public void setActionName(String actionName) {
            this.actionName = actionName;
        }

        public LocalDateTime getOperateTime() {
            return operateTime;
        }

        public void setOperateTime(LocalDateTime operateTime) {
            this.operateTime = operateTime;
        }

        public String getPeriodMonth() {
            return periodMonth;
        }

        public void setPeriodMonth(String periodMonth) {
            this.periodMonth = periodMonth;
        }

        public BigDecimal getFurnaceTempC() {
            return furnaceTempC;
        }

        public void setFurnaceTempC(BigDecimal furnaceTempC) {
            this.furnaceTempC = furnaceTempC;
        }

        public BigDecimal getProcessTon() {
            return processTon;
        }

        public void setProcessTon(BigDecimal processTon) {
            this.processTon = processTon;
        }

        public BigDecimal getPowerKwh() {
            return powerKwh;
        }

        public void setPowerKwh(BigDecimal powerKwh) {
            this.powerKwh = powerKwh;
        }

        public BigDecimal getRunHours() {
            return runHours;
        }

        public void setRunHours(BigDecimal runHours) {
            this.runHours = runHours;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }
}
