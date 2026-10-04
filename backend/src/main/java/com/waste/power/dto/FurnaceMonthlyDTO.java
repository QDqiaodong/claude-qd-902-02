package com.waste.power.dto;

import java.math.BigDecimal;

/**
 * JPQL 投影：按炉号 + 月份统计处理量与发电量。
 */
public class FurnaceMonthlyDTO {

    private String furnaceCode;
    private String periodMonth;
    private BigDecimal processTon;
    private BigDecimal powerKwh;
    private Long shifts;

    public FurnaceMonthlyDTO(String furnaceCode, String periodMonth, BigDecimal processTon, BigDecimal powerKwh,
            Long shifts) {
        this.furnaceCode = furnaceCode;
        this.periodMonth = periodMonth;
        this.processTon = processTon;
        this.powerKwh = powerKwh;
        this.shifts = shifts;
    }

    public String getFurnaceCode() {
        return furnaceCode;
    }

    public void setFurnaceCode(String furnaceCode) {
        this.furnaceCode = furnaceCode;
    }

    public String getPeriodMonth() {
        return periodMonth;
    }

    public void setPeriodMonth(String periodMonth) {
        this.periodMonth = periodMonth;
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

    public Long getShifts() {
        return shifts;
    }

    public void setShifts(Long shifts) {
        this.shifts = shifts;
    }
}
