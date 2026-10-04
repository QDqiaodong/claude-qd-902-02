package com.waste.power.dto;

import java.math.BigDecimal;

/**
 * JPQL 投影：按来源行政区划汇总进厂量（车次、净重合计、平均热值）。
 */
public class SourceSummaryDTO {

    private String sourceRegion;
    private Long trips;
    private BigDecimal netWeightTon;
    private Double avgCalorific;

    public SourceSummaryDTO(String sourceRegion, Long trips, BigDecimal netWeightTon, Double avgCalorific) {
        this.sourceRegion = sourceRegion;
        this.trips = trips;
        this.netWeightTon = netWeightTon;
        this.avgCalorific = avgCalorific;
    }

    public String getSourceRegion() {
        return sourceRegion;
    }

    public void setSourceRegion(String sourceRegion) {
        this.sourceRegion = sourceRegion;
    }

    public Long getTrips() {
        return trips;
    }

    public void setTrips(Long trips) {
        this.trips = trips;
    }

    public BigDecimal getNetWeightTon() {
        return netWeightTon;
    }

    public void setNetWeightTon(BigDecimal netWeightTon) {
        this.netWeightTon = netWeightTon;
    }

    public Double getAvgCalorific() {
        return avgCalorific;
    }

    public void setAvgCalorific(Double avgCalorific) {
        this.avgCalorific = avgCalorific;
    }
}
