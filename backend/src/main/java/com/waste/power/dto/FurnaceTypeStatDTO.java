package com.waste.power.dto;

import java.math.BigDecimal;

/**
 * 按焚烧炉子类（炉排炉 / 流化床 / 回转窑）的统计结果，由 JPQL TYPE() 查询汇总后填充。
 */
public class FurnaceTypeStatDTO {

    private String furnaceType;
    private String furnaceTypeName;
    private Long furnaceCount;
    private BigDecimal dailyProcessTon;
    private Double avgFurnaceTempC;
    private BigDecimal ratedCapacityTpd;

    public FurnaceTypeStatDTO() {
    }

    public FurnaceTypeStatDTO(String furnaceType, String furnaceTypeName, Long furnaceCount,
            BigDecimal dailyProcessTon, Double avgFurnaceTempC, BigDecimal ratedCapacityTpd) {
        this.furnaceType = furnaceType;
        this.furnaceTypeName = furnaceTypeName;
        this.furnaceCount = furnaceCount;
        this.dailyProcessTon = dailyProcessTon;
        this.avgFurnaceTempC = avgFurnaceTempC;
        this.ratedCapacityTpd = ratedCapacityTpd;
    }

    public String getFurnaceType() {
        return furnaceType;
    }

    public void setFurnaceType(String furnaceType) {
        this.furnaceType = furnaceType;
    }

    public String getFurnaceTypeName() {
        return furnaceTypeName;
    }

    public void setFurnaceTypeName(String furnaceTypeName) {
        this.furnaceTypeName = furnaceTypeName;
    }

    public Long getFurnaceCount() {
        return furnaceCount;
    }

    public void setFurnaceCount(Long furnaceCount) {
        this.furnaceCount = furnaceCount;
    }

    public BigDecimal getDailyProcessTon() {
        return dailyProcessTon;
    }

    public void setDailyProcessTon(BigDecimal dailyProcessTon) {
        this.dailyProcessTon = dailyProcessTon;
    }

    public Double getAvgFurnaceTempC() {
        return avgFurnaceTempC;
    }

    public void setAvgFurnaceTempC(Double avgFurnaceTempC) {
        this.avgFurnaceTempC = avgFurnaceTempC;
    }

    public BigDecimal getRatedCapacityTpd() {
        return ratedCapacityTpd;
    }

    public void setRatedCapacityTpd(BigDecimal ratedCapacityTpd) {
        this.ratedCapacityTpd = ratedCapacityTpd;
    }
}
