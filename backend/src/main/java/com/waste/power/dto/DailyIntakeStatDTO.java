package com.waste.power.dto;

import java.math.BigDecimal;

/**
 * JPQL 投影：指定时段内的进厂汇总（车次、净重合计、平均净重）。
 */
public class DailyIntakeStatDTO {

    private Long trips;
    private BigDecimal netWeightTon;
    private Double avgNetWeightTon;

    public DailyIntakeStatDTO(Long trips, BigDecimal netWeightTon, Double avgNetWeightTon) {
        this.trips = trips;
        this.netWeightTon = netWeightTon;
        this.avgNetWeightTon = avgNetWeightTon;
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

    public Double getAvgNetWeightTon() {
        return avgNetWeightTon;
    }

    public void setAvgNetWeightTon(Double avgNetWeightTon) {
        this.avgNetWeightTon = avgNetWeightTon;
    }
}
