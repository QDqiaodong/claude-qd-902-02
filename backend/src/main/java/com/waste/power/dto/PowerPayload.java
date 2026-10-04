package com.waste.power.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.waste.power.common.Dict;
import com.waste.power.domain.Boiler;
import com.waste.power.domain.PowerDailySettlement;
import com.waste.power.domain.PowerMeterTrace;
import com.waste.power.domain.TurbineGenerator;

/**
 * 余热锅炉与汽轮发电模块的请求与视图对象。
 */
public class PowerPayload {

    /** 锅炉视图 */
    public static class BoilerVO {

        private Long id;
        private String boilerNo;
        private String boilerName;
        private String furnaceCode;
        private BigDecimal steamPressureMpa;
        private BigDecimal steamTempC;
        private BigDecimal evaporationTph;
        private BigDecimal feedWaterTempC;
        private BigDecimal flueGasTempC;
        private BigDecimal ratedEvaporationTph;
        private String status;
        private String statusName;
        private String remark;
        /** 蒸发率 = 蒸发量 / 额定蒸发量 */
        private Double loadRate;

        public static BoilerVO from(Boiler boiler) {
            BoilerVO vo = new BoilerVO();
            vo.id = boiler.getId();
            vo.boilerNo = boiler.getBoilerNo();
            vo.boilerName = boiler.getBoilerName();
            vo.furnaceCode = boiler.getFurnaceCode();
            vo.steamPressureMpa = boiler.getSteamPressureMpa();
            vo.steamTempC = boiler.getSteamTempC();
            vo.evaporationTph = boiler.getEvaporationTph();
            vo.feedWaterTempC = boiler.getFeedWaterTempC();
            vo.flueGasTempC = boiler.getFlueGasTempC();
            vo.ratedEvaporationTph = boiler.getRatedEvaporationTph();
            vo.status = boiler.getStatus();
            vo.statusName = Dict.boilerStatusName(boiler.getStatus());
            vo.remark = boiler.getRemark();
            if (boiler.getRatedEvaporationTph() != null && boiler.getEvaporationTph() != null
                    && boiler.getRatedEvaporationTph().compareTo(BigDecimal.ZERO) > 0) {
                vo.loadRate = boiler.getEvaporationTph().doubleValue()
                        / boiler.getRatedEvaporationTph().doubleValue();
            } else {
                vo.loadRate = 0D;
            }
            return vo;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getBoilerNo() {
            return boilerNo;
        }

        public void setBoilerNo(String boilerNo) {
            this.boilerNo = boilerNo;
        }

        public String getBoilerName() {
            return boilerName;
        }

        public void setBoilerName(String boilerName) {
            this.boilerName = boilerName;
        }

        public String getFurnaceCode() {
            return furnaceCode;
        }

        public void setFurnaceCode(String furnaceCode) {
            this.furnaceCode = furnaceCode;
        }

        public BigDecimal getSteamPressureMpa() {
            return steamPressureMpa;
        }

        public void setSteamPressureMpa(BigDecimal steamPressureMpa) {
            this.steamPressureMpa = steamPressureMpa;
        }

        public BigDecimal getSteamTempC() {
            return steamTempC;
        }

        public void setSteamTempC(BigDecimal steamTempC) {
            this.steamTempC = steamTempC;
        }

        public BigDecimal getEvaporationTph() {
            return evaporationTph;
        }

        public void setEvaporationTph(BigDecimal evaporationTph) {
            this.evaporationTph = evaporationTph;
        }

        public BigDecimal getFeedWaterTempC() {
            return feedWaterTempC;
        }

        public void setFeedWaterTempC(BigDecimal feedWaterTempC) {
            this.feedWaterTempC = feedWaterTempC;
        }

        public BigDecimal getFlueGasTempC() {
            return flueGasTempC;
        }

        public void setFlueGasTempC(BigDecimal flueGasTempC) {
            this.flueGasTempC = flueGasTempC;
        }

        public BigDecimal getRatedEvaporationTph() {
            return ratedEvaporationTph;
        }

        public void setRatedEvaporationTph(BigDecimal ratedEvaporationTph) {
            this.ratedEvaporationTph = ratedEvaporationTph;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getStatusName() {
            return statusName;
        }

        public void setStatusName(String statusName) {
            this.statusName = statusName;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public Double getLoadRate() {
            return loadRate;
        }

        public void setLoadRate(Double loadRate) {
            this.loadRate = loadRate;
        }
    }

    /** 汽轮发电机视图（含理论发电上限与校核结果、当前表码与增量基数来源） */
    public static class TurbineVO {

        private Long id;
        private String unitNo;
        private String unitName;
        private BigDecimal capacityMw;
        private BigDecimal generationKwh;
        private BigDecimal gridKwh;
        private BigDecimal auxiliaryRate;
        private BigDecimal runningHours;
        /** 主电能表当前累计表码（期末抄见）kWh */
        private BigDecimal mainMeterReadingKwh;
        /** 上网关口表当前累计表码 kWh */
        private BigDecimal gridMeterReadingKwh;
        /** 当前主电能表表号 */
        private String meterNo;
        /** 增量基数来源代码 / 名称 */
        private String basisSource;
        private String basisSourceName;
        private LocalDate statDate;
        private String status;
        private String statusName;
        /** 最近一次日结的异常状态代码 / 名称 */
        private String anomalyStatus;
        private String anomalyStatusName;
        private String remark;
        /** 理论发电上限 = 装机 MW × 1000 × 当日运行小时 */
        private BigDecimal theoreticalMaxKwh;
        /** 容量因子 = 当日发电增量 / 理论上限 */
        private Double capacityFactor;

        public static TurbineVO from(TurbineGenerator unit, BigDecimal maxKwh, Double factor) {
            TurbineVO vo = new TurbineVO();
            vo.id = unit.getId();
            vo.unitNo = unit.getUnitNo();
            vo.unitName = unit.getUnitName();
            vo.capacityMw = unit.getCapacityMw();
            vo.generationKwh = unit.getGenerationKwh();
            vo.gridKwh = unit.getGridKwh();
            vo.auxiliaryRate = unit.getAuxiliaryRate();
            vo.runningHours = unit.getRunningHours();
            vo.mainMeterReadingKwh = unit.getMainMeterReadingKwh();
            vo.gridMeterReadingKwh = unit.getGridMeterReadingKwh();
            vo.meterNo = unit.getMeterNo();
            vo.basisSource = unit.getBasisSource();
            vo.basisSourceName = Dict.basisSourceName(unit.getBasisSource());
            vo.statDate = unit.getStatDate();
            vo.status = unit.getStatus();
            vo.statusName = Dict.unitStatusName(unit.getStatus());
            vo.remark = unit.getRemark();
            vo.theoreticalMaxKwh = maxKwh;
            vo.capacityFactor = factor;
            return vo;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUnitNo() {
            return unitNo;
        }

        public void setUnitNo(String unitNo) {
            this.unitNo = unitNo;
        }

        public String getUnitName() {
            return unitName;
        }

        public void setUnitName(String unitName) {
            this.unitName = unitName;
        }

        public BigDecimal getCapacityMw() {
            return capacityMw;
        }

        public void setCapacityMw(BigDecimal capacityMw) {
            this.capacityMw = capacityMw;
        }

        public BigDecimal getGenerationKwh() {
            return generationKwh;
        }

        public void setGenerationKwh(BigDecimal generationKwh) {
            this.generationKwh = generationKwh;
        }

        public BigDecimal getGridKwh() {
            return gridKwh;
        }

        public void setGridKwh(BigDecimal gridKwh) {
            this.gridKwh = gridKwh;
        }

        public BigDecimal getAuxiliaryRate() {
            return auxiliaryRate;
        }

        public void setAuxiliaryRate(BigDecimal auxiliaryRate) {
            this.auxiliaryRate = auxiliaryRate;
        }

        public BigDecimal getRunningHours() {
            return runningHours;
        }

        public void setRunningHours(BigDecimal runningHours) {
            this.runningHours = runningHours;
        }

        public BigDecimal getMainMeterReadingKwh() {
            return mainMeterReadingKwh;
        }

        public void setMainMeterReadingKwh(BigDecimal mainMeterReadingKwh) {
            this.mainMeterReadingKwh = mainMeterReadingKwh;
        }

        public BigDecimal getGridMeterReadingKwh() {
            return gridMeterReadingKwh;
        }

        public void setGridMeterReadingKwh(BigDecimal gridMeterReadingKwh) {
            this.gridMeterReadingKwh = gridMeterReadingKwh;
        }

        public String getMeterNo() {
            return meterNo;
        }

        public void setMeterNo(String meterNo) {
            this.meterNo = meterNo;
        }

        public String getBasisSource() {
            return basisSource;
        }

        public void setBasisSource(String basisSource) {
            this.basisSource = basisSource;
        }

        public String getBasisSourceName() {
            return basisSourceName;
        }

        public void setBasisSourceName(String basisSourceName) {
            this.basisSourceName = basisSourceName;
        }

        public String getAnomalyStatus() {
            return anomalyStatus;
        }

        public void setAnomalyStatus(String anomalyStatus) {
            this.anomalyStatus = anomalyStatus;
        }

        public String getAnomalyStatusName() {
            return anomalyStatusName;
        }

        public void setAnomalyStatusName(String anomalyStatusName) {
            this.anomalyStatusName = anomalyStatusName;
        }

        public LocalDate getStatDate() {
            return statDate;
        }

        public void setStatDate(LocalDate statDate) {
            this.statDate = statDate;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getStatusName() {
            return statusName;
        }

        public void setStatusName(String statusName) {
            this.statusName = statusName;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public BigDecimal getTheoreticalMaxKwh() {
            return theoreticalMaxKwh;
        }

        public void setTheoreticalMaxKwh(BigDecimal theoreticalMaxKwh) {
            this.theoreticalMaxKwh = theoreticalMaxKwh;
        }

        public Double getCapacityFactor() {
            return capacityFactor;
        }

        public void setCapacityFactor(Double capacityFactor) {
            this.capacityFactor = capacityFactor;
        }
    }

    /** 锅炉工况调整请求 */
    public static class BoilerRequest {

        private BigDecimal steamPressureMpa;
        private BigDecimal steamTempC;
        private BigDecimal evaporationTph;
        private String status;

        public BigDecimal getSteamPressureMpa() {
            return steamPressureMpa;
        }

        public void setSteamPressureMpa(BigDecimal steamPressureMpa) {
            this.steamPressureMpa = steamPressureMpa;
        }

        public BigDecimal getSteamTempC() {
            return steamTempC;
        }

        public void setSteamTempC(BigDecimal steamTempC) {
            this.steamTempC = steamTempC;
        }

        public BigDecimal getEvaporationTph() {
            return evaporationTph;
        }

        public void setEvaporationTph(BigDecimal evaporationTph) {
            this.evaporationTph = evaporationTph;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    // ============================== 发电日结（相邻表码增量口径） ==============================

    /** 日结登记请求：提交当日主电能表抄见表码，按相邻表码计算当日发电增量 */
    public static class DailySettleRequest {

        private LocalDate statDate;
        /** 主电能表当日期末（抄见）累计表码 kWh */
        private BigDecimal currentReadingKwh;
        /** 当日主电能表表号（换表时与当前表号不同） */
        private String mainMeterNo;
        /** 首次建档（无上日日结）时必须提供的期初基数 kWh */
        private BigDecimal initialBaseReadingKwh;
        /** 当日运行小时 h（不传按 24h 校核理论上限） */
        private BigDecimal runningHours;
        /** 上网电量三选一：直接给当日上网电量增量 kWh */
        private BigDecimal gridKwh;
        /** 上网电量三选一：上网关口表期初/期末累计表码，差值为当日上网电量 */
        private BigDecimal gridPrevReadingKwh;
        private BigDecimal gridCurrentReadingKwh;
        /** 表计回拨/更换时的处置选择 SEAL_NEW_BASE / SUSPEND */
        private String decision;
        /** 表计事件类型 ROLLBACK / METER_REPLACED（与 decision 同时出现） */
        private String meterEvent;
        /** 新表表号 / 新表启用基数（SEAL_NEW_BASE 时使用） */
        private String newMeterNo;
        private BigDecimal newMeterBaseReadingKwh;
        /** 封账或暂停理由（必填） */
        private String decisionReason;
        private String operator;

        public LocalDate getStatDate() {
            return statDate;
        }

        public void setStatDate(LocalDate statDate) {
            this.statDate = statDate;
        }

        public BigDecimal getCurrentReadingKwh() {
            return currentReadingKwh;
        }

        public void setCurrentReadingKwh(BigDecimal currentReadingKwh) {
            this.currentReadingKwh = currentReadingKwh;
        }

        public String getMainMeterNo() {
            return mainMeterNo;
        }

        public void setMainMeterNo(String mainMeterNo) {
            this.mainMeterNo = mainMeterNo;
        }

        public BigDecimal getInitialBaseReadingKwh() {
            return initialBaseReadingKwh;
        }

        public void setInitialBaseReadingKwh(BigDecimal initialBaseReadingKwh) {
            this.initialBaseReadingKwh = initialBaseReadingKwh;
        }

        public BigDecimal getRunningHours() {
            return runningHours;
        }

        public void setRunningHours(BigDecimal runningHours) {
            this.runningHours = runningHours;
        }

        public BigDecimal getGridKwh() {
            return gridKwh;
        }

        public void setGridKwh(BigDecimal gridKwh) {
            this.gridKwh = gridKwh;
        }

        public BigDecimal getGridPrevReadingKwh() {
            return gridPrevReadingKwh;
        }

        public void setGridPrevReadingKwh(BigDecimal gridPrevReadingKwh) {
            this.gridPrevReadingKwh = gridPrevReadingKwh;
        }

        public BigDecimal getGridCurrentReadingKwh() {
            return gridCurrentReadingKwh;
        }

        public void setGridCurrentReadingKwh(BigDecimal gridCurrentReadingKwh) {
            this.gridCurrentReadingKwh = gridCurrentReadingKwh;
        }

        public String getDecision() {
            return decision;
        }

        public void setDecision(String decision) {
            this.decision = decision;
        }

        public String getMeterEvent() {
            return meterEvent;
        }

        public void setMeterEvent(String meterEvent) {
            this.meterEvent = meterEvent;
        }

        public String getNewMeterNo() {
            return newMeterNo;
        }

        public void setNewMeterNo(String newMeterNo) {
            this.newMeterNo = newMeterNo;
        }

        public BigDecimal getNewMeterBaseReadingKwh() {
            return newMeterBaseReadingKwh;
        }

        public void setNewMeterBaseReadingKwh(BigDecimal newMeterBaseReadingKwh) {
            this.newMeterBaseReadingKwh = newMeterBaseReadingKwh;
        }

        public String getDecisionReason() {
            return decisionReason;
        }

        public void setDecisionReason(String decisionReason) {
            this.decisionReason = decisionReason;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }
    }

    /** 校表请求：暂停日结（SUSPENDED）后登记现场校表确认的有效基数 */
    public static class MeterVerifyRequest {

        private Integer version;
        private BigDecimal confirmedReadingKwh;
        private String verifyNote;
        private String operator;

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }

        public BigDecimal getConfirmedReadingKwh() {
            return confirmedReadingKwh;
        }

        public void setConfirmedReadingKwh(BigDecimal confirmedReadingKwh) {
            this.confirmedReadingKwh = confirmedReadingKwh;
        }

        public String getVerifyNote() {
            return verifyNote;
        }

        public void setVerifyNote(String verifyNote) {
            this.verifyNote = verifyNote;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }
    }

    /** 发电日结视图：列表展示当前表码、当日增量、基数来源与异常状态，并保留每天计算依据 */
    public static class DailySettlementVO {

        private Long id;
        private String unitNo;
        private String unitName;
        private java.time.LocalDate statDate;
        private String status;
        private String statusName;
        private String mainMeterNo;
        private BigDecimal prevReadingKwh;
        private BigDecimal currentReadingKwh;
        private String basisSource;
        private String basisSourceName;
        private BigDecimal generationKwh;
        private String gridMeterNo;
        private BigDecimal gridPrevReadingKwh;
        private BigDecimal gridCurrentReadingKwh;
        private BigDecimal gridKwh;
        private BigDecimal auxiliaryRate;
        private BigDecimal runningHours;
        private String meterEvent;
        private String meterEventName;
        private String anomalyStatus;
        private String anomalyStatusName;
        private BigDecimal oldMeterSealedReadingKwh;
        private String newMeterNo;
        private BigDecimal newMeterBaseReadingKwh;
        private String decision;
        private String decisionName;
        private String decisionReason;
        private String decidedBy;
        private java.time.LocalDateTime decidedTime;
        private BigDecimal verifiedReadingKwh;
        private String verifiedBy;
        private java.time.LocalDateTime verifiedTime;
        private String verifyNote;
        private String operator;
        private String remark;
        private Integer version;
        private java.time.LocalDateTime settleTime;

        public static DailySettlementVO from(PowerDailySettlement s) {
            DailySettlementVO vo = new DailySettlementVO();
            vo.id = s.getId();
            vo.unitNo = s.getUnitNo();
            vo.statDate = s.getStatDate();
            vo.status = s.getStatus();
            vo.statusName = Dict.settlementStatusName(s.getStatus());
            vo.mainMeterNo = s.getMainMeterNo();
            vo.prevReadingKwh = s.getPrevReadingKwh();
            vo.currentReadingKwh = s.getCurrentReadingKwh();
            vo.basisSource = s.getBasisSource();
            vo.basisSourceName = Dict.basisSourceName(s.getBasisSource());
            vo.generationKwh = s.getGenerationKwh();
            vo.gridMeterNo = s.getGridMeterNo();
            vo.gridPrevReadingKwh = s.getGridPrevReadingKwh();
            vo.gridCurrentReadingKwh = s.getGridCurrentReadingKwh();
            vo.gridKwh = s.getGridKwh();
            vo.auxiliaryRate = s.getAuxiliaryRate();
            vo.runningHours = s.getRunningHours();
            vo.meterEvent = s.getMeterEvent();
            vo.meterEventName = Dict.meterEventName(s.getMeterEvent());
            vo.anomalyStatus = s.getAnomalyStatus();
            vo.anomalyStatusName = Dict.meterAnomalyName(s.getAnomalyStatus());
            vo.oldMeterSealedReadingKwh = s.getOldMeterSealedReadingKwh();
            vo.newMeterNo = s.getNewMeterNo();
            vo.newMeterBaseReadingKwh = s.getNewMeterBaseReadingKwh();
            vo.decision = s.getDecision();
            vo.decisionName = Dict.meterDecisionName(s.getDecision());
            vo.decisionReason = s.getDecisionReason();
            vo.decidedBy = s.getDecidedBy();
            vo.decidedTime = s.getDecidedTime();
            vo.verifiedReadingKwh = s.getVerifiedReadingKwh();
            vo.verifiedBy = s.getVerifiedBy();
            vo.verifiedTime = s.getVerifiedTime();
            vo.verifyNote = s.getVerifyNote();
            vo.operator = s.getOperator();
            vo.remark = s.getRemark();
            vo.version = s.getVersion();
            vo.settleTime = s.getSettleTime();
            return vo;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUnitNo() {
            return unitNo;
        }

        public void setUnitNo(String unitNo) {
            this.unitNo = unitNo;
        }

        public String getUnitName() {
            return unitName;
        }

        public void setUnitName(String unitName) {
            this.unitName = unitName;
        }

        public java.time.LocalDate getStatDate() {
            return statDate;
        }

        public void setStatDate(java.time.LocalDate statDate) {
            this.statDate = statDate;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getStatusName() {
            return statusName;
        }

        public void setStatusName(String statusName) {
            this.statusName = statusName;
        }

        public String getMainMeterNo() {
            return mainMeterNo;
        }

        public void setMainMeterNo(String mainMeterNo) {
            this.mainMeterNo = mainMeterNo;
        }

        public BigDecimal getPrevReadingKwh() {
            return prevReadingKwh;
        }

        public void setPrevReadingKwh(BigDecimal prevReadingKwh) {
            this.prevReadingKwh = prevReadingKwh;
        }

        public BigDecimal getCurrentReadingKwh() {
            return currentReadingKwh;
        }

        public void setCurrentReadingKwh(BigDecimal currentReadingKwh) {
            this.currentReadingKwh = currentReadingKwh;
        }

        public String getBasisSource() {
            return basisSource;
        }

        public void setBasisSource(String basisSource) {
            this.basisSource = basisSource;
        }

        public String getBasisSourceName() {
            return basisSourceName;
        }

        public void setBasisSourceName(String basisSourceName) {
            this.basisSourceName = basisSourceName;
        }

        public BigDecimal getGenerationKwh() {
            return generationKwh;
        }

        public void setGenerationKwh(BigDecimal generationKwh) {
            this.generationKwh = generationKwh;
        }

        public String getGridMeterNo() {
            return gridMeterNo;
        }

        public void setGridMeterNo(String gridMeterNo) {
            this.gridMeterNo = gridMeterNo;
        }

        public BigDecimal getGridPrevReadingKwh() {
            return gridPrevReadingKwh;
        }

        public void setGridPrevReadingKwh(BigDecimal gridPrevReadingKwh) {
            this.gridPrevReadingKwh = gridPrevReadingKwh;
        }

        public BigDecimal getGridCurrentReadingKwh() {
            return gridCurrentReadingKwh;
        }

        public void setGridCurrentReadingKwh(BigDecimal gridCurrentReadingKwh) {
            this.gridCurrentReadingKwh = gridCurrentReadingKwh;
        }

        public BigDecimal getGridKwh() {
            return gridKwh;
        }

        public void setGridKwh(BigDecimal gridKwh) {
            this.gridKwh = gridKwh;
        }

        public BigDecimal getAuxiliaryRate() {
            return auxiliaryRate;
        }

        public void setAuxiliaryRate(BigDecimal auxiliaryRate) {
            this.auxiliaryRate = auxiliaryRate;
        }

        public BigDecimal getRunningHours() {
            return runningHours;
        }

        public void setRunningHours(BigDecimal runningHours) {
            this.runningHours = runningHours;
        }

        public String getMeterEvent() {
            return meterEvent;
        }

        public void setMeterEvent(String meterEvent) {
            this.meterEvent = meterEvent;
        }

        public String getMeterEventName() {
            return meterEventName;
        }

        public void setMeterEventName(String meterEventName) {
            this.meterEventName = meterEventName;
        }

        public String getAnomalyStatus() {
            return anomalyStatus;
        }

        public void setAnomalyStatus(String anomalyStatus) {
            this.anomalyStatus = anomalyStatus;
        }

        public String getAnomalyStatusName() {
            return anomalyStatusName;
        }

        public void setAnomalyStatusName(String anomalyStatusName) {
            this.anomalyStatusName = anomalyStatusName;
        }

        public BigDecimal getOldMeterSealedReadingKwh() {
            return oldMeterSealedReadingKwh;
        }

        public void setOldMeterSealedReadingKwh(BigDecimal oldMeterSealedReadingKwh) {
            this.oldMeterSealedReadingKwh = oldMeterSealedReadingKwh;
        }

        public String getNewMeterNo() {
            return newMeterNo;
        }

        public void setNewMeterNo(String newMeterNo) {
            this.newMeterNo = newMeterNo;
        }

        public BigDecimal getNewMeterBaseReadingKwh() {
            return newMeterBaseReadingKwh;
        }

        public void setNewMeterBaseReadingKwh(BigDecimal newMeterBaseReadingKwh) {
            this.newMeterBaseReadingKwh = newMeterBaseReadingKwh;
        }

        public String getDecision() {
            return decision;
        }

        public void setDecision(String decision) {
            this.decision = decision;
        }

        public String getDecisionName() {
            return decisionName;
        }

        public void setDecisionName(String decisionName) {
            this.decisionName = decisionName;
        }

        public String getDecisionReason() {
            return decisionReason;
        }

        public void setDecisionReason(String decisionReason) {
            this.decisionReason = decisionReason;
        }

        public String getDecidedBy() {
            return decidedBy;
        }

        public void setDecidedBy(String decidedBy) {
            this.decidedBy = decidedBy;
        }

        public java.time.LocalDateTime getDecidedTime() {
            return decidedTime;
        }

        public void setDecidedTime(java.time.LocalDateTime decidedTime) {
            this.decidedTime = decidedTime;
        }

        public BigDecimal getVerifiedReadingKwh() {
            return verifiedReadingKwh;
        }

        public void setVerifiedReadingKwh(BigDecimal verifiedReadingKwh) {
            this.verifiedReadingKwh = verifiedReadingKwh;
        }

        public String getVerifiedBy() {
            return verifiedBy;
        }

        public void setVerifiedBy(String verifiedBy) {
            this.verifiedBy = verifiedBy;
        }

        public java.time.LocalDateTime getVerifiedTime() {
            return verifiedTime;
        }

        public void setVerifiedTime(java.time.LocalDateTime verifiedTime) {
            this.verifiedTime = verifiedTime;
        }

        public String getVerifyNote() {
            return verifyNote;
        }

        public void setVerifyNote(String verifyNote) {
            this.verifyNote = verifyNote;
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

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }

        public java.time.LocalDateTime getSettleTime() {
            return settleTime;
        }

        public void setSettleTime(java.time.LocalDateTime settleTime) {
            this.settleTime = settleTime;
        }
    }

    /** 日结/表计处置操作轨迹视图 */
    public static class MeterTraceVO {

        private Long id;
        private String unitNo;
        private java.time.LocalDate statDate;
        private Long settlementId;
        private String action;
        private String fromStatus;
        private String toStatus;
        private String operatorName;
        private String result;
        private String detail;
        private java.time.LocalDateTime operateTime;

        public static MeterTraceVO from(PowerMeterTrace t) {
            MeterTraceVO vo = new MeterTraceVO();
            vo.id = t.getId();
            vo.unitNo = t.getUnitNo();
            vo.statDate = t.getStatDate();
            vo.settlementId = t.getSettlementId();
            vo.action = t.getAction();
            vo.fromStatus = t.getFromStatus();
            vo.toStatus = t.getToStatus();
            vo.operatorName = t.getOperatorName();
            vo.result = t.getResult();
            vo.detail = t.getDetail();
            vo.operateTime = t.getOperateTime();
            return vo;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUnitNo() {
            return unitNo;
        }

        public void setUnitNo(String unitNo) {
            this.unitNo = unitNo;
        }

        public java.time.LocalDate getStatDate() {
            return statDate;
        }

        public void setStatDate(java.time.LocalDate statDate) {
            this.statDate = statDate;
        }

        public Long getSettlementId() {
            return settlementId;
        }

        public void setSettlementId(Long settlementId) {
            this.settlementId = settlementId;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getFromStatus() {
            return fromStatus;
        }

        public void setFromStatus(String fromStatus) {
            this.fromStatus = fromStatus;
        }

        public String getToStatus() {
            return toStatus;
        }

        public void setToStatus(String toStatus) {
            this.toStatus = toStatus;
        }

        public String getOperatorName() {
            return operatorName;
        }

        public void setOperatorName(String operatorName) {
            this.operatorName = operatorName;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }

        public java.time.LocalDateTime getOperateTime() {
            return operateTime;
        }

        public void setOperateTime(java.time.LocalDateTime operateTime) {
            this.operateTime = operateTime;
        }
    }
}
