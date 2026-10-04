package com.waste.power.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * 炉排炉（机械炉排炉）：专属参数为炉排速度、料层厚度、炉排段数。
 */
@Entity
@DiscriminatorValue("GRATE")
public class GrateFurnace extends Incinerator {

    /** 炉排速度 m/h */
    @Column(name = "grate_speed_mh", precision = 6, scale = 2)
    private BigDecimal grateSpeedMh;

    /** 料层厚度 mm */
    @Column(name = "grate_bed_thickness_mm", precision = 8, scale = 1)
    private BigDecimal grateBedThicknessMm;

    /** 炉排段数 */
    @Column(name = "grate_stage_count")
    private Integer grateStageCount;

    /** 一次风温 ℃ */
    @Column(name = "primary_air_temp_c", precision = 6, scale = 1)
    private BigDecimal primaryAirTempC;

    public BigDecimal getGrateSpeedMh() {
        return grateSpeedMh;
    }

    public void setGrateSpeedMh(BigDecimal grateSpeedMh) {
        this.grateSpeedMh = grateSpeedMh;
    }

    public BigDecimal getGrateBedThicknessMm() {
        return grateBedThicknessMm;
    }

    public void setGrateBedThicknessMm(BigDecimal grateBedThicknessMm) {
        this.grateBedThicknessMm = grateBedThicknessMm;
    }

    public Integer getGrateStageCount() {
        return grateStageCount;
    }

    public void setGrateStageCount(Integer grateStageCount) {
        this.grateStageCount = grateStageCount;
    }

    public BigDecimal getPrimaryAirTempC() {
        return primaryAirTempC;
    }

    public void setPrimaryAirTempC(BigDecimal primaryAirTempC) {
        this.primaryAirTempC = primaryAirTempC;
    }
}
