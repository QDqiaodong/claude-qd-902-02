package com.waste.power.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.waste.power.common.Dict;
import com.waste.power.domain.AbnormalDisposal;
import com.waste.power.domain.AbnormalDisposalAlarm;
import com.waste.power.domain.AbnormalDisposalTrace;
import com.waste.power.domain.EnvAlarm;

/**
 * 异常工况处置单模块的请求与视图对象。
 */
public class DisposalPayload {

    /** 人工发起处置单 */
    public static class OpenRequest {

        private String furnaceCode;
        private String ownerName;
        private String reason;

        public String getFurnaceCode() {
            return furnaceCode;
        }

        public void setFurnaceCode(String furnaceCode) {
            this.furnaceCode = furnaceCode;
        }

        public String getOwnerName() {
            return ownerName;
        }

        public void setOwnerName(String ownerName) {
            this.ownerName = ownerName;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    /** 负责人记录处置措施（可同时申请复核，进入待复核） */
    public static class MeasureRequest {

        private String ownerName;
        private String measureNote;
        /** 是否记录后立即申请复核 */
        private Boolean requestReview;
        /** 乐观锁版本：过期版本提交直接拒绝 */
        private Integer version;

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

        public Boolean getRequestReview() {
            return requestReview;
        }

        public void setRequestReview(Boolean requestReview) {
            this.requestReview = requestReview;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }
    }

    /** 另一名复核人员执行复核恢复（或拒绝并退回处置中） */
    public static class ReviewRequest {

        private String reviewerName;
        private String reviewNote;
        /** RECOVER 确认达标且严重告警解除，执行恢复；REJECT 不满足条件退回处置中 */
        private String choice;
        private Integer version;

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

        public String getChoice() {
            return choice;
        }

        public void setChoice(String choice) {
            this.choice = choice;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }
    }

    /** 告警数据暂不可用时的明确选择 */
    public static class MissingChoiceRequest {

        /** WAIT_FREEZE 继续冻结等待补齐 / CLOSE_KEEP_FROZEN 记录理由后关闭处置但保持投料冻结 */
        private String choice;
        private String reason;
        private String operatorName;
        private Integer version;

        public String getChoice() {
            return choice;
        }

        public void setChoice(String choice) {
            this.choice = choice;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getOperatorName() {
            return operatorName;
        }

        public void setOperatorName(String operatorName) {
            this.operatorName = operatorName;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }
    }

    /** 关闭已恢复处置单（解冻投料） */
    public static class CloseRequest {

        private String operatorName;
        private Integer version;

        public String getOperatorName() {
            return operatorName;
        }

        public void setOperatorName(String operatorName) {
            this.operatorName = operatorName;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }
    }

    /** 处置单视图（列表/详情共用，详情额外携带证据、轨迹、实时恢复条件） */
    public static class VO {

        private Long id;
        private String disposalNo;
        private String furnaceCode;
        private String furnaceName;
        private String status;
        private String statusName;
        private String triggerSource;
        private String triggerSourceName;
        private List<String> triggerReasons;

        private BigDecimal snapshotFurnaceTempC;
        private BigDecimal snapshotResidenceS;
        private BigDecimal snapshotCoPpm;
        private String snapshotCombustion;
        private BigDecimal snapshotDailyTon;
        private String snapshotStackCode;
        private String snapshotGrabNo;
        private String snapshotPitNo;
        private String snapshotPitGrabNo;
        private BigDecimal snapshotPitStockTon;

        private Boolean feedFrozen;
        private LocalDateTime frozenTime;
        private LocalDateTime unfrozenTime;

        private String ownerName;
        private String measureNote;
        private LocalDateTime handledTime;
        private LocalDateTime reviewRequestTime;

        private String reviewerName;
        private String reviewNote;
        private BigDecimal recoverFurnaceTempC;
        private BigDecimal recoverResidenceS;

        private String alarmDataStatus;
        private String missingChoice;
        private String missingChoiceName;
        private String missingReason;
        private String missingDecidedBy;
        private LocalDateTime missingDecidedTime;

        private Integer version;
        private LocalDateTime closeTime;
        private String remark;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;

        /** 关联严重告警证据条数 */
        private Integer alarmEvidenceCount;
        /** 实时投料是否冻结（与炉维度联锁一致，刷新/多人操作后以此为准） */
        private Boolean feedBlockedNow;

        public static VO from(AbnormalDisposal d, List<String> reasons) {
            VO vo = new VO();
            vo.id = d.getId();
            vo.disposalNo = d.getDisposalNo();
            vo.furnaceCode = d.getFurnaceCode();
            vo.furnaceName = d.getFurnaceName();
            vo.status = d.getStatus();
            vo.statusName = Dict.disposalStatusName(d.getStatus());
            vo.triggerSource = d.getTriggerSource();
            vo.triggerSourceName = Dict.disposalSourceName(d.getTriggerSource());
            vo.triggerReasons = reasons;
            vo.snapshotFurnaceTempC = d.getSnapshotFurnaceTempC();
            vo.snapshotResidenceS = d.getSnapshotResidenceS();
            vo.snapshotCoPpm = d.getSnapshotCoPpm();
            vo.snapshotCombustion = d.getSnapshotCombustion();
            vo.snapshotDailyTon = d.getSnapshotDailyTon();
            vo.snapshotStackCode = d.getSnapshotStackCode();
            vo.snapshotGrabNo = d.getSnapshotGrabNo();
            vo.snapshotPitNo = d.getSnapshotPitNo();
            vo.snapshotPitGrabNo = d.getSnapshotPitGrabNo();
            vo.snapshotPitStockTon = d.getSnapshotPitStockTon();
            vo.feedFrozen = d.getFeedFrozen();
            vo.frozenTime = d.getFrozenTime();
            vo.unfrozenTime = d.getUnfrozenTime();
            vo.ownerName = d.getOwnerName();
            vo.measureNote = d.getMeasureNote();
            vo.handledTime = d.getHandledTime();
            vo.reviewRequestTime = d.getReviewRequestTime();
            vo.reviewerName = d.getReviewerName();
            vo.reviewNote = d.getReviewNote();
            vo.recoverFurnaceTempC = d.getRecoverFurnaceTempC();
            vo.recoverResidenceS = d.getRecoverResidenceS();
            vo.alarmDataStatus = d.getAlarmDataStatus();
            vo.missingChoice = d.getMissingChoice();
            vo.missingChoiceName = Dict.missingChoiceName(d.getMissingChoice());
            vo.missingReason = d.getMissingReason();
            vo.missingDecidedBy = d.getMissingDecidedBy();
            vo.missingDecidedTime = d.getMissingDecidedTime();
            vo.version = d.getVersion();
            vo.closeTime = d.getCloseTime();
            vo.remark = d.getRemark();
            vo.createTime = d.getCreateTime();
            vo.updateTime = d.getUpdateTime();
            return vo;
        }

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

        public String getStatusName() {
            return statusName;
        }

        public void setStatusName(String statusName) {
            this.statusName = statusName;
        }

        public String getTriggerSource() {
            return triggerSource;
        }

        public void setTriggerSource(String triggerSource) {
            this.triggerSource = triggerSource;
        }

        public String getTriggerSourceName() {
            return triggerSourceName;
        }

        public void setTriggerSourceName(String triggerSourceName) {
            this.triggerSourceName = triggerSourceName;
        }

        public List<String> getTriggerReasons() {
            return triggerReasons;
        }

        public void setTriggerReasons(List<String> triggerReasons) {
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

        public String getMissingChoiceName() {
            return missingChoiceName;
        }

        public void setMissingChoiceName(String missingChoiceName) {
            this.missingChoiceName = missingChoiceName;
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

        public Integer getAlarmEvidenceCount() {
            return alarmEvidenceCount;
        }

        public void setAlarmEvidenceCount(Integer alarmEvidenceCount) {
            this.alarmEvidenceCount = alarmEvidenceCount;
        }

        public Boolean getFeedBlockedNow() {
            return feedBlockedNow;
        }

        public void setFeedBlockedNow(Boolean feedBlockedNow) {
            this.feedBlockedNow = feedBlockedNow;
        }
    }

    /** 处置单-告警证据视图 */
    public static class AlarmEvidenceVO {

        private Long linkId;
        private Long alarmId;
        private String alarmNo;
        private String indicatorCode;
        private String indicatorName;
        private BigDecimal measuredValue;
        private BigDecimal limitValue;
        private BigDecimal exceedRatio;
        private String alarmLevel;
        private String alarmLevelName;
        private String periodLabel;
        private String stackCode;
        private LocalDateTime alarmTime;
        private Boolean handled;
        private String handleNote;
        private String bindSource;
        private String bindSourceName;
        private LocalDateTime bindTime;

        public static AlarmEvidenceVO from(AbnormalDisposalAlarm link, EnvAlarm alarm) {
            AlarmEvidenceVO vo = new AlarmEvidenceVO();
            vo.linkId = link.getId();
            vo.bindSource = link.getBindSource();
            vo.bindSourceName = Dict.evidenceBindName(link.getBindSource());
            vo.bindTime = link.getBindTime();
            vo.alarmId = alarm.getId();
            vo.alarmNo = alarm.getAlarmNo();
            vo.indicatorCode = alarm.getIndicatorCode();
            vo.indicatorName = alarm.getIndicatorName();
            vo.measuredValue = alarm.getMeasuredValue();
            vo.limitValue = alarm.getLimitValue();
            vo.exceedRatio = alarm.getExceedRatio();
            vo.alarmLevel = alarm.getAlarmLevel();
            vo.alarmLevelName = Dict.alarmLevelName(alarm.getAlarmLevel());
            vo.periodLabel = alarm.getPeriodLabel();
            vo.stackCode = alarm.getStackCode();
            vo.alarmTime = alarm.getAlarmTime();
            vo.handled = alarm.getHandled();
            vo.handleNote = alarm.getHandleNote();
            return vo;
        }

        public Long getLinkId() {
            return linkId;
        }

        public void setLinkId(Long linkId) {
            this.linkId = linkId;
        }

        public Long getAlarmId() {
            return alarmId;
        }

        public void setAlarmId(Long alarmId) {
            this.alarmId = alarmId;
        }

        public String getAlarmNo() {
            return alarmNo;
        }

        public void setAlarmNo(String alarmNo) {
            this.alarmNo = alarmNo;
        }

        public String getIndicatorCode() {
            return indicatorCode;
        }

        public void setIndicatorCode(String indicatorCode) {
            this.indicatorCode = indicatorCode;
        }

        public String getIndicatorName() {
            return indicatorName;
        }

        public void setIndicatorName(String indicatorName) {
            this.indicatorName = indicatorName;
        }

        public BigDecimal getMeasuredValue() {
            return measuredValue;
        }

        public void setMeasuredValue(BigDecimal measuredValue) {
            this.measuredValue = measuredValue;
        }

        public BigDecimal getLimitValue() {
            return limitValue;
        }

        public void setLimitValue(BigDecimal limitValue) {
            this.limitValue = limitValue;
        }

        public BigDecimal getExceedRatio() {
            return exceedRatio;
        }

        public void setExceedRatio(BigDecimal exceedRatio) {
            this.exceedRatio = exceedRatio;
        }

        public String getAlarmLevel() {
            return alarmLevel;
        }

        public void setAlarmLevel(String alarmLevel) {
            this.alarmLevel = alarmLevel;
        }

        public String getAlarmLevelName() {
            return alarmLevelName;
        }

        public void setAlarmLevelName(String alarmLevelName) {
            this.alarmLevelName = alarmLevelName;
        }

        public String getPeriodLabel() {
            return periodLabel;
        }

        public void setPeriodLabel(String periodLabel) {
            this.periodLabel = periodLabel;
        }

        public String getStackCode() {
            return stackCode;
        }

        public void setStackCode(String stackCode) {
            this.stackCode = stackCode;
        }

        public LocalDateTime getAlarmTime() {
            return alarmTime;
        }

        public void setAlarmTime(LocalDateTime alarmTime) {
            this.alarmTime = alarmTime;
        }

        public Boolean getHandled() {
            return handled;
        }

        public void setHandled(Boolean handled) {
            this.handled = handled;
        }

        public String getHandleNote() {
            return handleNote;
        }

        public void setHandleNote(String handleNote) {
            this.handleNote = handleNote;
        }

        public String getBindSource() {
            return bindSource;
        }

        public void setBindSource(String bindSource) {
            this.bindSource = bindSource;
        }

        public String getBindSourceName() {
            return bindSourceName;
        }

        public void setBindSourceName(String bindSourceName) {
            this.bindSourceName = bindSourceName;
        }

        public LocalDateTime getBindTime() {
            return bindTime;
        }

        public void setBindTime(LocalDateTime bindTime) {
            this.bindTime = bindTime;
        }
    }

    /** 操作轨迹视图 */
    public static class TraceVO {

        private Long id;
        private String action;
        private String fromStatus;
        private String fromStatusName;
        private String toStatus;
        private String toStatusName;
        private String operatorName;
        private String result;
        private String detail;
        private LocalDateTime operateTime;

        public static TraceVO from(AbnormalDisposalTrace t) {
            TraceVO vo = new TraceVO();
            vo.id = t.getId();
            vo.action = t.getAction();
            vo.fromStatus = t.getFromStatus();
            vo.fromStatusName = Dict.disposalStatusName(t.getFromStatus());
            vo.toStatus = t.getToStatus();
            vo.toStatusName = Dict.disposalStatusName(t.getToStatus());
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

        public String getFromStatusName() {
            return fromStatusName;
        }

        public void setFromStatusName(String fromStatusName) {
            this.fromStatusName = fromStatusName;
        }

        public String getToStatus() {
            return toStatus;
        }

        public void setToStatus(String toStatus) {
            this.toStatus = toStatus;
        }

        public String getToStatusName() {
            return toStatusName;
        }

        public void setToStatusName(String toStatusName) {
            this.toStatusName = toStatusName;
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

        public LocalDateTime getOperateTime() {
            return operateTime;
        }

        public void setOperateTime(LocalDateTime operateTime) {
            this.operateTime = operateTime;
        }
    }

    /** 实时恢复条件评估：详情页展示恢复条件影响，复核恢复接口同样按此判定，防误放行 */
    public static class ConditionVO {

        private String furnaceCode;
        private BigDecimal currentFurnaceTempC;
        private BigDecimal currentResidenceS;
        private Boolean combustionQualified;
        private List<String> unqualifiedItems;
        private Boolean severeAlarmActive;
        private Integer severeAlarmCount;
        private String alarmDataStatus;
        private String missingChoice;
        private String missingChoiceName;
        private Boolean feedBlocked;
        /** 是否满足复核恢复条件（燃烧达标 且 严重告警解除 且 未被缺失选择关闭） */
        private Boolean canRecover;
        /** 不满足时逐项列出阻塞原因（恢复期间新到严重告警也会在此出现） */
        private List<String> blockers;

        public String getFurnaceCode() {
            return furnaceCode;
        }

        public void setFurnaceCode(String furnaceCode) {
            this.furnaceCode = furnaceCode;
        }

        public BigDecimal getCurrentFurnaceTempC() {
            return currentFurnaceTempC;
        }

        public void setCurrentFurnaceTempC(BigDecimal currentFurnaceTempC) {
            this.currentFurnaceTempC = currentFurnaceTempC;
        }

        public BigDecimal getCurrentResidenceS() {
            return currentResidenceS;
        }

        public void setCurrentResidenceS(BigDecimal currentResidenceS) {
            this.currentResidenceS = currentResidenceS;
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

        public Boolean getSevereAlarmActive() {
            return severeAlarmActive;
        }

        public void setSevereAlarmActive(Boolean severeAlarmActive) {
            this.severeAlarmActive = severeAlarmActive;
        }

        public Integer getSevereAlarmCount() {
            return severeAlarmCount;
        }

        public void setSevereAlarmCount(Integer severeAlarmCount) {
            this.severeAlarmCount = severeAlarmCount;
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

        public String getMissingChoiceName() {
            return missingChoiceName;
        }

        public void setMissingChoiceName(String missingChoiceName) {
            this.missingChoiceName = missingChoiceName;
        }

        public Boolean getFeedBlocked() {
            return feedBlocked;
        }

        public void setFeedBlocked(Boolean feedBlocked) {
            this.feedBlocked = feedBlocked;
        }

        public Boolean getCanRecover() {
            return canRecover;
        }

        public void setCanRecover(Boolean canRecover) {
            this.canRecover = canRecover;
        }

        public List<String> getBlockers() {
            return blockers;
        }

        public void setBlockers(List<String> blockers) {
            this.blockers = blockers;
        }
    }
}
