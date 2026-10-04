package com.waste.power.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 余热锅炉：蒸汽压力、蒸汽温度、蒸发量、对应炉号、给水温度、状态。
 */
@Entity
@Table(name = "boiler")
public class Boiler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "boiler_no", nullable = false, unique = true, length = 32)
    private String boilerNo;

    @Column(name = "boiler_name", length = 64)
    private String boilerName;

    /** 对应焚烧炉炉号 */
    @Column(name = "furnace_code", length = 32)
    private String furnaceCode;

    /** 蒸汽压力 MPa */
    @Column(name = "steam_pressure_mpa", precision = 6, scale = 2)
    private BigDecimal steamPressureMpa;

    /** 蒸汽温度 ℃ */
    @Column(name = "steam_temp_c", precision = 6, scale = 1)
    private BigDecimal steamTempC;

    /** 蒸发量 t/h */
    @Column(name = "evaporation_tph", precision = 8, scale = 2)
    private BigDecimal evaporationTph;

    /** 给水温度 ℃ */
    @Column(name = "feed_water_temp_c", precision = 6, scale = 1)
    private BigDecimal feedWaterTempC;

    /** 排烟温度 ℃ */
    @Column(name = "flue_gas_temp_c", precision = 6, scale = 1)
    private BigDecimal flueGasTempC;

    /** 额定蒸发量 t/h */
    @Column(name = "rated_evaporation_tph", precision = 8, scale = 2)
    private BigDecimal ratedEvaporationTph;

    @Column(name = "status", length = 16)
    private String status;

    @Column(name = "remark", length = 128)
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBoilerNo() {
        return boilerNo;
    }

    public void setBoilerNo(String boilerNo) {
        this.boilerNo = boilerNo;
    }

    public String getBoilerName() {
        return boilerName;
    }

    public void setBoilerName(String boilerName) {
        this.boilerName = boilerName;
    }

    public String getFurnaceCode() {
        return furnaceCode;
    }

    public void setFurnaceCode(String furnaceCode) {
        this.furnaceCode = furnaceCode;
    }

    public BigDecimal getSteamPressureMpa() {
        return steamPressureMpa;
    }

    public void setSteamPressureMpa(BigDecimal steamPressureMpa) {
        this.steamPressureMpa = steamPressureMpa;
    }

    public BigDecimal getSteamTempC() {
        return steamTempC;
    }

    public void setSteamTempC(BigDecimal steamTempC) {
        this.steamTempC = steamTempC;
    }

    public BigDecimal getEvaporationTph() {
        return evaporationTph;
    }

    public void setEvaporationTph(BigDecimal evaporationTph) {
        this.evaporationTph = evaporationTph;
    }

    public BigDecimal getFeedWaterTempC() {
        return feedWaterTempC;
    }

    public void setFeedWaterTempC(BigDecimal feedWaterTempC) {
        this.feedWaterTempC = feedWaterTempC;
    }

    public BigDecimal getFlueGasTempC() {
        return flueGasTempC;
    }

    public void setFlueGasTempC(BigDecimal flueGasTempC) {
        this.flueGasTempC = flueGasTempC;
    }

    public BigDecimal getRatedEvaporationTph() {
        return ratedEvaporationTph;
    }

    public void setRatedEvaporationTph(BigDecimal ratedEvaporationTph) {
        this.ratedEvaporationTph = ratedEvaporationTph;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
