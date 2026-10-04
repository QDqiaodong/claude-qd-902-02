package com.waste.power.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 环保告警：任一排放指标超标时由环境服务自动登记，记录超标指标、时段、倍率与等级。
 */
@Entity
@Table(name = "env_alarm")
public class EnvAlarm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alarm_no", unique = true, length = 32)
    private String alarmNo;

    @Column(name = "indicator_code", length = 16)
    private String indicatorCode;

    @Column(name = "indicator_name", length = 32)
    private String indicatorName;

    @Column(name = "measured_value", precision = 12, scale = 3)
    private BigDecimal measuredValue;

    @Column(name = "limit_value", precision = 12, scale = 3)
    private BigDecimal limitValue;

    /** 超标倍率 % */
    @Column(name = "exceed_ratio", precision = 8, scale = 2)
    private BigDecimal exceedRatio;

    /** 等级：MINOR / MAJOR / SEVERE */
    @Column(name = "alarm_level", length = 16)
    private String alarmLevel;

    @Column(name = "period_label", length = 64)
    private String periodLabel;

    @Column(name = "stack_code", length = 32)
    private String stackCode;

    @Column(name = "alarm_time")
    private LocalDateTime alarmTime;

    @Column(name = "handled")
    private Boolean handled;

    @Column(name = "handle_note", length = 128)
    private String handleNote;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
