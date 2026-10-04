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
 * 烟气排放指标记录：颗粒物/SO2/NOx/HCl/CO/二噁英 的实测值、限值、是否超标、监测时段。
 */
@Entity
@Table(name = "emission_record")
public class EmissionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 指标代码：PARTICULATE / SO2 / NOX / HCL / CO / DIOXIN */
    @Column(name = "indicator_code", nullable = false, length = 16)
    private String indicatorCode;

    @Column(name = "indicator_name", length = 32)
    private String indicatorName;

    /** 实测值 */
    @Column(name = "measured_value", precision = 12, scale = 3)
    private BigDecimal measuredValue;

    /** 限值 */
    @Column(name = "limit_value", precision = 12, scale = 3)
    private BigDecimal limitValue;

    @Column(name = "unit", length = 16)
    private String unit;

    @Column(name = "exceeded")
    private Boolean exceeded;

    /** 超标倍率 % */
    @Column(name = "exceed_ratio", precision = 8, scale = 2)
    private BigDecimal exceedRatio;

    /** 监测时段，如 2026-09-27 08:00~12:00 */
    @Column(name = "period_label", length = 64)
    private String periodLabel;

    @Column(name = "monitor_time")
    private LocalDateTime monitorTime;

    /** 排放口 / 对应炉号 */
    @Column(name = "stack_code", length = 32)
    private String stackCode;

    /** 排放标记：NORMAL 正常排放 / EXCEED 超标排放 */
    @Column(name = "emission_flag", length = 16)
    private String emissionFlag;

    @Column(name = "remark", length = 128)
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Boolean getExceeded() {
        return exceeded;
    }

    public void setExceeded(Boolean exceeded) {
        this.exceeded = exceeded;
    }

    public BigDecimal getExceedRatio() {
        return exceedRatio;
    }

    public void setExceedRatio(BigDecimal exceedRatio) {
        this.exceedRatio = exceedRatio;
    }

    public String getPeriodLabel() {
        return periodLabel;
    }

    public void setPeriodLabel(String periodLabel) {
        this.periodLabel = periodLabel;
    }

    public LocalDateTime getMonitorTime() {
        return monitorTime;
    }

    public void setMonitorTime(LocalDateTime monitorTime) {
        this.monitorTime = monitorTime;
    }

    public String getStackCode() {
        return stackCode;
    }

    public void setStackCode(String stackCode) {
        this.stackCode = stackCode;
    }

    public String getEmissionFlag() {
        return emissionFlag;
    }

    public void setEmissionFlag(String emissionFlag) {
        this.emissionFlag = emissionFlag;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
