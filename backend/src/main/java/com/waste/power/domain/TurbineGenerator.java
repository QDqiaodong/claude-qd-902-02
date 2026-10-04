package com.waste.power.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 汽轮发电机组：装机容量、当日发电增量/上网电量/厂用电率（日结口径）、运行小时，
 * 以及主电能表与上网关口表的当前累计表码、表号与增量基数来源。
 *
 * 注意：generation_kwh/grid_kwh 存的是最近统计日的【当日增量】，累计值只在 *_meter_reading_kwh 表码列，
 * 日结逻辑见 PowerDailySettlement。
 */
@Entity
@Table(name = "turbine_generator")
public class TurbineGenerator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unit_no", nullable = false, unique = true, length = 32)
    private String unitNo;

    @Column(name = "unit_name", length = 64)
    private String unitName;

    /** 装机容量 MW */
    @Column(name = "capacity_mw", precision = 8, scale = 2)
    private BigDecimal capacityMw;

    /** 最近统计日发电增量 kWh（日结口径，不是累计表码） */
    @Column(name = "generation_kwh", precision = 14, scale = 2)
    private BigDecimal generationKwh;

    /** 最近统计日上网电量 kWh */
    @Column(name = "grid_kwh", precision = 14, scale = 2)
    private BigDecimal gridKwh;

    /** 最近统计日厂用电率 % */
    @Column(name = "auxiliary_rate", precision = 5, scale = 2)
    private BigDecimal auxiliaryRate;

    /** 最近统计日运行小时 h */
    @Column(name = "running_hours", precision = 10, scale = 1)
    private BigDecimal runningHours;

    /** 主电能表当前累计表码 kWh（期末抄见） */
    @Column(name = "main_meter_reading_kwh", precision = 16, scale = 2)
    private BigDecimal mainMeterReadingKwh;

    /** 上网关口表当前累计表码 kWh */
    @Column(name = "grid_meter_reading_kwh", precision = 16, scale = 2)
    private BigDecimal gridMeterReadingKwh;

    /** 当前主电能表表号 */
    @Column(name = "meter_no", length = 32)
    private String meterNo;

    /** 当前增量基数来源 PREV_DAY/INIT_BASE/NEW_METER_BASE/VERIFIED_BASE */
    @Column(name = "basis_source", length = 32)
    private String basisSource;

    @Column(name = "stat_date")
    private LocalDate statDate;

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

    public String getUnitNo() {
        return unitNo;
    }

    public void setUnitNo(String unitNo) {
        this.unitNo = unitNo;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public BigDecimal getCapacityMw() {
        return capacityMw;
    }

    public void setCapacityMw(BigDecimal capacityMw) {
        this.capacityMw = capacityMw;
    }

    public BigDecimal getGenerationKwh() {
        return generationKwh;
    }

    public void setGenerationKwh(BigDecimal generationKwh) {
        this.generationKwh = generationKwh;
    }

    public BigDecimal getGridKwh() {
        return gridKwh;
    }

    public void setGridKwh(BigDecimal gridKwh) {
        this.gridKwh = gridKwh;
    }

    public BigDecimal getAuxiliaryRate() {
        return auxiliaryRate;
    }

    public void setAuxiliaryRate(BigDecimal auxiliaryRate) {
        this.auxiliaryRate = auxiliaryRate;
    }

    public BigDecimal getRunningHours() {
        return runningHours;
    }

    public void setRunningHours(BigDecimal runningHours) {
        this.runningHours = runningHours;
    }

    public BigDecimal getMainMeterReadingKwh() {
        return mainMeterReadingKwh;
    }

    public void setMainMeterReadingKwh(BigDecimal mainMeterReadingKwh) {
        this.mainMeterReadingKwh = mainMeterReadingKwh;
    }

    public BigDecimal getGridMeterReadingKwh() {
        return gridMeterReadingKwh;
    }

    public void setGridMeterReadingKwh(BigDecimal gridMeterReadingKwh) {
        this.gridMeterReadingKwh = gridMeterReadingKwh;
    }

    public String getMeterNo() {
        return meterNo;
    }

    public void setMeterNo(String meterNo) {
        this.meterNo = meterNo;
    }

    public String getBasisSource() {
        return basisSource;
    }

    public void setBasisSource(String basisSource) {
        this.basisSource = basisSource;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
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
