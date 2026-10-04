package com.waste.power.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * 流化床焚烧炉：专属参数为床料高度、流化风速、床温、石英砂床料粒径。
 */
@Entity
@DiscriminatorValue("FLUIDIZED")
public class FluidizedBed extends Incinerator {

    /** 床料高度 mm */
    @Column(name = "bed_material_height_mm", precision = 8, scale = 1)
    private BigDecimal bedMaterialHeightMm;

    /** 流化风速 m/s */
    @Column(name = "fluidizing_air_speed_ms", precision = 6, scale = 2)
    private BigDecimal fluidizingAirSpeedMs;

    /** 床温 ℃ */
    @Column(name = "bed_temp_c", precision = 6, scale = 1)
    private BigDecimal bedTempC;

    /** 床料（石英砂）粒径 mm */
    @Column(name = "bed_material_size_mm", precision = 6, scale = 2)
    private BigDecimal bedMaterialSizeMm;

    public BigDecimal getBedMaterialHeightMm() {
        return bedMaterialHeightMm;
    }

    public void setBedMaterialHeightMm(BigDecimal bedMaterialHeightMm) {
        this.bedMaterialHeightMm = bedMaterialHeightMm;
    }

    public BigDecimal getFluidizingAirSpeedMs() {
        return fluidizingAirSpeedMs;
    }

    public void setFluidizingAirSpeedMs(BigDecimal fluidizingAirSpeedMs) {
        this.fluidizingAirSpeedMs = fluidizingAirSpeedMs;
    }

    public BigDecimal getBedTempC() {
        return bedTempC;
    }

    public void setBedTempC(BigDecimal bedTempC) {
        this.bedTempC = bedTempC;
    }

    public BigDecimal getBedMaterialSizeMm() {
        return bedMaterialSizeMm;
    }

    public void setBedMaterialSizeMm(BigDecimal bedMaterialSizeMm) {
        this.bedMaterialSizeMm = bedMaterialSizeMm;
    }
}
