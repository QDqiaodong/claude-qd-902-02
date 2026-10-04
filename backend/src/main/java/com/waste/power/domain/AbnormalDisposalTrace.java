package com.waste.power.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 处置单操作轨迹：创建、措施记录、申请复核、复核恢复/拒绝、关闭、证据合并、数据缺失选择，
 * 以及重复点击 / 过期版本 / 复核条件不满足等失败尝试，全部留痕，闭环可追溯。
 */
@Entity
@Table(name = "abnormal_disposal_trace")
public class AbnormalDisposalTrace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "disposal_id", nullable = false)
    private Long disposalId;

    @Column(name = "disposal_no", nullable = false, length = 40)
    private String disposalNo;

    /** CREATE / MEASURE / REQUEST_REVIEW / REVIEW_RECOVER / CLOSE / MERGE_ALARM / DATA_MISSING_CHOICE / REJECT / FAIL */
    @Column(name = "action", nullable = false, length = 32)
    private String action;

    @Column(name = "from_status", length = 16)
    private String fromStatus;

    @Column(name = "to_status", length = 16)
    private String toStatus;

    @Column(name = "operator_name", length = 32)
    private String operatorName;

    /** SUCCESS / FAILED */
    @Column(name = "result", nullable = false, length = 16)
    private String result;

    @Column(name = "detail", length = 512)
    private String detail;

    @Column(name = "operate_time", nullable = false)
    private LocalDateTime operateTime;

    public AbnormalDisposalTrace() {
    }

    public AbnormalDisposalTrace(Long disposalId, String disposalNo, String action, String fromStatus,
            String toStatus, String operatorName, String result, String detail, LocalDateTime operateTime) {
        this.disposalId = disposalId;
        this.disposalNo = disposalNo;
        this.action = action;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.operatorName = operatorName;
        this.result = result;
        this.detail = detail;
        this.operateTime = operateTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDisposalId() {
        return disposalId;
    }

    public void setDisposalId(Long disposalId) {
        this.disposalId = disposalId;
    }

    public String getDisposalNo() {
        return disposalNo;
    }

    public void setDisposalNo(String disposalNo) {
        this.disposalNo = disposalNo;
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

    public LocalDateTime getOperateTime() {
        return operateTime;
    }

    public void setOperateTime(LocalDateTime operateTime) {
        this.operateTime = operateTime;
    }
}
