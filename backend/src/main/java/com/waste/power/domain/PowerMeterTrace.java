package com.waste.power.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 发电日结与表计处置操作轨迹：每次日结、表计回拨/更换的封账或暂停决策、校表确认，
 * 以及重复请求幂等返回、覆盖较新日结被拒、过期页面提交等失败尝试全部留痕。
 *
 * 与 PowerDailySettlement 弱关联（settlement_id 可为空），按机组 + 时间可审计。
 */
@Entity
@Table(name = "power_meter_trace")
public class PowerMeterTrace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unit_no", nullable = false, length = 32)
    private String unitNo;

    @Column(name = "stat_date")
    private LocalDate statDate;

    @Column(name = "settlement_id")
    private Long settlementId;

    /** SETTLE / IDEMPOTENT_RETRY / REJECT_DUPLICATE / REJECT_STALE_DATE / SEAL_NEW_BASE / SUSPEND / VERIFY / FAIL */
    @Column(name = "action", nullable = false, length = 40)
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

    public PowerMeterTrace() {
    }

    public PowerMeterTrace(String unitNo, LocalDate statDate, Long settlementId, String action,
            String fromStatus, String toStatus, String operatorName, String result,
            String detail, LocalDateTime operateTime) {
        this.unitNo = unitNo;
        this.statDate = statDate;
        this.settlementId = settlementId;
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

    public LocalDateTime getOperateTime() {
        return operateTime;
    }

    public void setOperateTime(LocalDateTime operateTime) {
        this.operateTime = operateTime;
    }
}
