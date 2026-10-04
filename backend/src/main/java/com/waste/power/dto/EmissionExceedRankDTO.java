package com.waste.power.dto;

import java.math.BigDecimal;

/**
 * JPQL 投影：排放指标超标次数排行。
 */
public class EmissionExceedRankDTO {

    private String indicatorCode;
    private String indicatorName;
    private Long exceedCount;
    private BigDecimal maxMeasuredValue;
    private BigDecimal limitValue;

    public EmissionExceedRankDTO(String indicatorCode, Long exceedCount, BigDecimal maxMeasuredValue,
            BigDecimal limitValue) {
        this.indicatorCode = indicatorCode;
        this.exceedCount = exceedCount;
        this.maxMeasuredValue = maxMeasuredValue;
        this.limitValue = limitValue;
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

    public Long getExceedCount() {
        return exceedCount;
    }

    public void setExceedCount(Long exceedCount) {
        this.exceedCount = exceedCount;
    }

    public BigDecimal getMaxMeasuredValue() {
        return maxMeasuredValue;
    }

    public void setMaxMeasuredValue(BigDecimal maxMeasuredValue) {
        this.maxMeasuredValue = maxMeasuredValue;
    }

    public BigDecimal getLimitValue() {
        return limitValue;
    }

    public void setLimitValue(BigDecimal limitValue) {
        this.limitValue = limitValue;
    }
}
