package com.waste.power.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * 汽轮发电机组发电日结：按机组 + 统计日唯一，用相邻表码（期末 − 期初基数）计算当日发电增量，
 * 再算上网电量与厂用电率，并完整保留每天的计算依据。
 *
 * 状态机：SETTLED 已日结（终态，机组/日期唯一不允许改）；
 *   SUSPENDED 表计回拨/更换时选择「暂停日结等待校表」建立的挂起记录；
 *   VERIFIED 挂起记录经现场校表确认有效基数后，等待对该日重新日结。
 *
 * 表计异常处置（meter_event 非 NONE 时必须二选一并留痕）：
 *   SEAL_NEW_BASE 旧表封账并启用新基数：旧表在 old_meter_sealed_reading_kwh 封账，
 *     后续增量口径锚定 new_meter_no / new_meter_base_reading_kwh；
 *   SUSPEND 暂停日结等待校表：当日不出增量，机组后续日期的日结一律拦住，校表后方可继续。
 *
 * 并发口径：机组行锁（TurbineGenerator.lockByUnitNo）把同机组日结串行化，
 *   uk_unit_stat_date 唯一索引库级兜底重复请求；@Version 拦截过期页面重复校表。
 */
@Entity
@Table(name = "power_daily_settlement")
public class PowerDailySettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unit_no", nullable = false, length = 32)
    private String unitNo;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    /** SETTLED / SUSPENDED / VERIFIED */
    @Column(name = "status", nullable = false, length = 16)
    private String status;

    // ---------------- 主电能表表码与当日增量 ----------------

    @Column(name = "main_meter_no", length = 32)
    private String mainMeterNo;

    /** 期初（相邻）表码 kWh */
    @Column(name = "prev_reading_kwh", precision = 16, scale = 2)
    private BigDecimal prevReadingKwh;

    /** 期末（当日抄见）表码 kWh */
    @Column(name = "current_reading_kwh", precision = 16, scale = 2)
    private BigDecimal currentReadingKwh;

    /** 增量基数来源 PREV_DAY / INIT_BASE / NEW_METER_BASE / VERIFIED_BASE */
    @Column(name = "basis_source", nullable = false, length = 32)
    private String basisSource;

    /** 当日发电增量 = 期末表码 − 期初基数 kWh */
    @Column(name = "generation_kwh", precision = 14, scale = 2)
    private BigDecimal generationKwh;

    // ---------------- 上网关口表与当日上网电量 ----------------

    @Column(name = "grid_meter_no", length = 32)
    private String gridMeterNo;

    @Column(name = "grid_prev_reading_kwh", precision = 16, scale = 2)
    private BigDecimal gridPrevReadingKwh;

    @Column(name = "grid_current_reading_kwh", precision = 16, scale = 2)
    private BigDecimal gridCurrentReadingKwh;

    @Column(name = "grid_kwh", precision = 14, scale = 2)
    private BigDecimal gridKwh;

    /** 厂用电率 % */
    @Column(name = "auxiliary_rate", precision = 5, scale = 2)
    private BigDecimal auxiliaryRate;

    /** 当日运行小时 h */
    @Column(name = "running_hours", precision = 10, scale = 1)
    private BigDecimal runningHours;

    // ---------------- 表计回拨 / 更换处置 ----------------

    /** NONE / ROLLBACK / METER_REPLACED */
    @Column(name = "meter_event", length = 32)
    private String meterEvent;

    /** NORMAL / METER_ROLLBACK / METER_REPLACED / SUSPENDED */
    @Column(name = "anomaly_status", nullable = false, length = 32)
    private String anomalyStatus;

    @Column(name = "old_meter_sealed_reading_kwh", precision = 16, scale = 2)
    private BigDecimal oldMeterSealedReadingKwh;

    @Column(name = "new_meter_no", length = 32)
    private String newMeterNo;

    @Column(name = "new_meter_base_reading_kwh", precision = 16, scale = 2)
    private BigDecimal newMeterBaseReadingKwh;

    /** SEAL_NEW_BASE / SUSPEND */
    @Column(name = "decision", length = 32)
    private String decision;

    @Column(name = "decision_reason", length = 255)
    private String decisionReason;

    @Column(name = "decided_by", length = 32)
    private String decidedBy;

    @Column(name = "decided_time")
    private LocalDateTime decidedTime;

    // ---------------- 暂停日结后的校表登记 ----------------

    @Column(name = "verified_reading_kwh", precision = 16, scale = 2)
    private BigDecimal verifiedReadingKwh;

    @Column(name = "verified_by", length = 32)
    private String verifiedBy;

    @Column(name = "verified_time")
    private LocalDateTime verifiedTime;

    @Column(name = "verify_note", length = 255)
    private String verifyNote;

    // ---------------- 审计与并发 ----------------

    @Column(name = "operator", length = 32)
    private String operator;

    @Column(name = "remark", length = 255)
    private String remark;

    @Column(name = "request_fingerprint", length = 64)
    private String requestFingerprint;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "settle_time")
    private LocalDateTime settleTime;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

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

    public String getAnomalyStatus() {
        return anomalyStatus;
    }

    public void setAnomalyStatus(String anomalyStatus) {
        this.anomalyStatus = anomalyStatus;
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

    public LocalDateTime getDecidedTime() {
        return decidedTime;
    }

    public void setDecidedTime(LocalDateTime decidedTime) {
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

    public LocalDateTime getVerifiedTime() {
        return verifiedTime;
    }

    public void setVerifiedTime(LocalDateTime verifiedTime) {
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

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public void setRequestFingerprint(String requestFingerprint) {
        this.requestFingerprint = requestFingerprint;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getSettleTime() {
        return settleTime;
    }

    public void setSettleTime(LocalDateTime settleTime) {
        this.settleTime = settleTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
