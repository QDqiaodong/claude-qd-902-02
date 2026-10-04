package com.waste.power.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
/**
 * 异常工况处置单：把「焚烧炉燃烧判定 → 同炉严重排放告警 → 储坑供料抓斗快照 → 投料冻结 →
 * 人工处置 → 他人复核恢复」串成一条可追溯闭环。
 *
 * 状态机：PENDING 待处置 → HANDLING 处置中 → REVIEWING 待复核 → RECOVERED 已恢复 → CLOSED 已关闭。
 * 同一炉未恢复（未关闭）前只允许存在一张有效处置单，由 active_once 生成列 + 唯一索引在库级兜底。
 * 并发提交由 @Version 乐观锁拦截（重复点击 / 过期版本 / 两人同时复核）。
 *
 * 注意：处置单只保存快照与操作轨迹，不改动任何已完成的启停炉与运行记录。
 */
@Entity
@Table(name = "abnormal_disposal")
public class AbnormalDisposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "disposal_no", nullable = false, unique = true, length = 40)
    private String disposalNo;

    @Column(name = "furnace_code", nullable = false, length = 32)
    private String furnaceCode;

    @Column(name = "furnace_name", length = 64)
    private String furnaceName;

    /** PENDING / HANDLING / REVIEWING / RECOVERED / CLOSED */
    @Column(name = "status", nullable = false, length = 16)
    private String status;

    /** 首发来源：AUTO 自动触发 / MANUAL 人工发起 */
    @Column(name = "trigger_source", nullable = false, length = 16)
    private String triggerSource;

    /** 触发原因（不达标项 / 同炉严重告警等），JSON 数组字符串 */
    @Column(name = "trigger_reasons", length = 512)
    private String triggerReasons;

    // ---------------- 炉况快照 ----------------

    @Column(name = "snapshot_furnace_temp_c", precision = 8, scale = 1)
    private BigDecimal snapshotFurnaceTempC;

    @Column(name = "snapshot_residence_s", precision = 6, scale = 2)
    private BigDecimal snapshotResidenceS;

    @Column(name = "snapshot_co_ppm", precision = 8, scale = 2)
    private BigDecimal snapshotCoPpm;

    @Column(name = "snapshot_combustion", length = 16)
    private String snapshotCombustion;

    @Column(name = "snapshot_daily_ton", precision = 10, scale = 2)
    private BigDecimal snapshotDailyTon;

    @Column(name = "snapshot_stack_code", length = 32)
    private String snapshotStackCode;

    // ---------------- 供料抓斗快照 ----------------

    @Column(name = "snapshot_grab_no", length = 16)
    private String snapshotGrabNo;

    @Column(name = "snapshot_pit_no", length = 16)
    private String snapshotPitNo;

    @Column(name = "snapshot_pit_grab_no", length = 16)
    private String snapshotPitGrabNo;

    @Column(name = "snapshot_pit_stock_ton", precision = 10, scale = 2)
    private BigDecimal snapshotPitStockTon;

    // ---------------- 投料联锁 ----------------

    @Column(name = "feed_frozen", nullable = false)
    private Boolean feedFrozen;

    @Column(name = "frozen_time")
    private LocalDateTime frozenTime;

    @Column(name = "unfrozen_time")
    private LocalDateTime unfrozenTime;

    // ---------------- 人工处置 ----------------

    @Column(name = "owner_name", length = 32)
    private String ownerName;

    @Column(name = "measure_note", length = 512)
    private String measureNote;

    @Column(name = "handled_time")
    private LocalDateTime handledTime;

    @Column(name = "review_request_time")
    private LocalDateTime reviewRequestTime;

    // ---------------- 复核恢复 ----------------

    @Column(name = "reviewer_name", length = 32)
    private String reviewerName;

    @Column(name = "review_note", length = 512)
    private String reviewNote;

    @Column(name = "recover_furnace_temp_c", precision = 8, scale = 1)
    private BigDecimal recoverFurnaceTempC;

    @Column(name = "recover_residence_s", precision = 6, scale = 2)
    private BigDecimal recoverResidenceS;

    // ---------------- 告警数据缺失处置 ----------------

    /** AVAILABLE 数据可用 / UNAVAILABLE 数据暂不可用 */
    @Column(name = "alarm_data_status", nullable = false, length = 16)
    private String alarmDataStatus;

    /** WAIT_FREEZE 继续冻结等待补齐 / CLOSE_KEEP_FROZEN 记录理由后关闭处置但保持投料冻结 */
    @Column(name = "missing_choice", length = 32)
    private String missingChoice;

    @Column(name = "missing_reason", length = 255)
    private String missingReason;

    @Column(name = "missing_decided_by", length = 32)
    private String missingDecidedBy;

    @Column(name = "missing_decided_time")
    private LocalDateTime missingDecidedTime;

    // ---------------- 审计字段 ----------------

    /** 乐观锁：过期版本提交、重复点击、两人同时复核均据此判定 */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "close_time")
    private LocalDateTime closeTime;

    @Column(name = "remark", length = 255)
    private String remark;

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

    public String getDisposalNo() {
        return disposalNo;
    }

    public void setDisposalNo(String disposalNo) {
        this.disposalNo = disposalNo;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(String triggerSource) {
        this.triggerSource = triggerSource;
    }

    public String getTriggerReasons() {
        return triggerReasons;
    }

    public void setTriggerReasons(String triggerReasons) {
        this.triggerReasons = triggerReasons;
    }

    public BigDecimal getSnapshotFurnaceTempC() {
        return snapshotFurnaceTempC;
    }

    public void setSnapshotFurnaceTempC(BigDecimal snapshotFurnaceTempC) {
        this.snapshotFurnaceTempC = snapshotFurnaceTempC;
    }

    public BigDecimal getSnapshotResidenceS() {
        return snapshotResidenceS;
    }

    public void setSnapshotResidenceS(BigDecimal snapshotResidenceS) {
        this.snapshotResidenceS = snapshotResidenceS;
    }

    public BigDecimal getSnapshotCoPpm() {
        return snapshotCoPpm;
    }

    public void setSnapshotCoPpm(BigDecimal snapshotCoPpm) {
        this.snapshotCoPpm = snapshotCoPpm;
    }

    public String getSnapshotCombustion() {
        return snapshotCombustion;
    }

    public void setSnapshotCombustion(String snapshotCombustion) {
        this.snapshotCombustion = snapshotCombustion;
    }

    public BigDecimal getSnapshotDailyTon() {
        return snapshotDailyTon;
    }

    public void setSnapshotDailyTon(BigDecimal snapshotDailyTon) {
        this.snapshotDailyTon = snapshotDailyTon;
    }

    public String getSnapshotStackCode() {
        return snapshotStackCode;
    }

    public void setSnapshotStackCode(String snapshotStackCode) {
        this.snapshotStackCode = snapshotStackCode;
    }

    public String getSnapshotGrabNo() {
        return snapshotGrabNo;
    }

    public void setSnapshotGrabNo(String snapshotGrabNo) {
        this.snapshotGrabNo = snapshotGrabNo;
    }

    public String getSnapshotPitNo() {
        return snapshotPitNo;
    }

    public void setSnapshotPitNo(String snapshotPitNo) {
        this.snapshotPitNo = snapshotPitNo;
    }

    public String getSnapshotPitGrabNo() {
        return snapshotPitGrabNo;
    }

    public void setSnapshotPitGrabNo(String snapshotPitGrabNo) {
        this.snapshotPitGrabNo = snapshotPitGrabNo;
    }

    public BigDecimal getSnapshotPitStockTon() {
        return snapshotPitStockTon;
    }

    public void setSnapshotPitStockTon(BigDecimal snapshotPitStockTon) {
        this.snapshotPitStockTon = snapshotPitStockTon;
    }

    public Boolean getFeedFrozen() {
        return feedFrozen;
    }

    public void setFeedFrozen(Boolean feedFrozen) {
        this.feedFrozen = feedFrozen;
    }

    public LocalDateTime getFrozenTime() {
        return frozenTime;
    }

    public void setFrozenTime(LocalDateTime frozenTime) {
        this.frozenTime = frozenTime;
    }

    public LocalDateTime getUnfrozenTime() {
        return unfrozenTime;
    }

    public void setUnfrozenTime(LocalDateTime unfrozenTime) {
        this.unfrozenTime = unfrozenTime;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getMeasureNote() {
        return measureNote;
    }

    public void setMeasureNote(String measureNote) {
        this.measureNote = measureNote;
    }

    public LocalDateTime getHandledTime() {
        return handledTime;
    }

    public void setHandledTime(LocalDateTime handledTime) {
        this.handledTime = handledTime;
    }

    public LocalDateTime getReviewRequestTime() {
        return reviewRequestTime;
    }

    public void setReviewRequestTime(LocalDateTime reviewRequestTime) {
        this.reviewRequestTime = reviewRequestTime;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public BigDecimal getRecoverFurnaceTempC() {
        return recoverFurnaceTempC;
    }

    public void setRecoverFurnaceTempC(BigDecimal recoverFurnaceTempC) {
        this.recoverFurnaceTempC = recoverFurnaceTempC;
    }

    public BigDecimal getRecoverResidenceS() {
        return recoverResidenceS;
    }

    public void setRecoverResidenceS(BigDecimal recoverResidenceS) {
        this.recoverResidenceS = recoverResidenceS;
    }

    public String getAlarmDataStatus() {
        return alarmDataStatus;
    }

    public void setAlarmDataStatus(String alarmDataStatus) {
        this.alarmDataStatus = alarmDataStatus;
    }

    public String getMissingChoice() {
        return missingChoice;
    }

    public void setMissingChoice(String missingChoice) {
        this.missingChoice = missingChoice;
    }

    public String getMissingReason() {
        return missingReason;
    }

    public void setMissingReason(String missingReason) {
        this.missingReason = missingReason;
    }

    public String getMissingDecidedBy() {
        return missingDecidedBy;
    }

    public void setMissingDecidedBy(String missingDecidedBy) {
        this.missingDecidedBy = missingDecidedBy;
    }

    public LocalDateTime getMissingDecidedTime() {
        return missingDecidedTime;
    }

    public void setMissingDecidedTime(LocalDateTime missingDecidedTime) {
        this.missingDecidedTime = missingDecidedTime;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(LocalDateTime closeTime) {
        this.closeTime = closeTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
