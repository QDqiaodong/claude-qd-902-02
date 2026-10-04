package com.waste.power.dto;

import java.math.BigDecimal;

/**
 * JPQL 投影：按垃圾类别汇总净重与车次（页面环形图占比用）。
 */
public class CategoryShareDTO {

    private String wasteCategory;
    private BigDecimal netWeightTon;
    private Long trips;

    public CategoryShareDTO(String wasteCategory, BigDecimal netWeightTon, Long trips) {
        this.wasteCategory = wasteCategory;
        this.netWeightTon = netWeightTon;
        this.trips = trips;
    }

    public String getWasteCategory() {
        return wasteCategory;
    }

    public void setWasteCategory(String wasteCategory) {
        this.wasteCategory = wasteCategory;
    }

    public BigDecimal getNetWeightTon() {
        return netWeightTon;
    }

    public void setNetWeightTon(BigDecimal netWeightTon) {
        this.netWeightTon = netWeightTon;
    }

    public Long getTrips() {
        return trips;
    }

    public void setTrips(Long trips) {
        this.trips = trips;
    }
}
