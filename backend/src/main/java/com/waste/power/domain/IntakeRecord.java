package com.waste.power.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 进厂车次与地磅计量：车牌、来源行政区划/中转站、垃圾类别、毛重/皮重/净重、进厂时间、地磅号。
 * 净重由 service 计算（净重 = 毛重 − 皮重），实体自身不设默认值。
 */
@Entity
@Table(name = "intake_record")
public class IntakeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "truck_no", nullable = false, length = 32)
    private String truckNo;

    @Column(name = "source_region", nullable = false, length = 32)
    private String sourceRegion;

    @Column(name = "transfer_station", length = 64)
    private String transferStation;

    @Column(name = "waste_category", nullable = false, length = 16)
    private String wasteCategory;

    @Column(name = "gross_weight_ton", precision = 10, scale = 2)
    private BigDecimal grossWeightTon;

    @Column(name = "tare_weight_ton", precision = 10, scale = 2)
    private BigDecimal tareWeightTon;

    @Column(name = "net_weight_ton", precision = 10, scale = 2)
    private BigDecimal netWeightTon;

    @Column(name = "intake_time")
    private LocalDateTime intakeTime;

    @Column(name = "scale_no", length = 16)
    private String scaleNo;

    /** 入场垃圾低位热值 kJ/kg */
    @Column(name = "calorific_value", precision = 10, scale = 0)
    private BigDecimal calorificValue;

    @Column(name = "driver_name", length = 32)
    private String driverName;

    @Column(name = "unload_pit", length = 16)
    private String unloadPit;

    @Column(name = "remark", length = 128)
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTruckNo() {
        return truckNo;
    }

    public void setTruckNo(String truckNo) {
        this.truckNo = truckNo;
    }

    public String getSourceRegion() {
        return sourceRegion;
    }

    public void setSourceRegion(String sourceRegion) {
        this.sourceRegion = sourceRegion;
    }

    public String getTransferStation() {
        return transferStation;
    }

    public void setTransferStation(String transferStation) {
        this.transferStation = transferStation;
    }

    public String getWasteCategory() {
        return wasteCategory;
    }

    public void setWasteCategory(String wasteCategory) {
        this.wasteCategory = wasteCategory;
    }

    public BigDecimal getGrossWeightTon() {
        return grossWeightTon;
    }

    public void setGrossWeightTon(BigDecimal grossWeightTon) {
        this.grossWeightTon = grossWeightTon;
    }

    public BigDecimal getTareWeightTon() {
        return tareWeightTon;
    }

    public void setTareWeightTon(BigDecimal tareWeightTon) {
        this.tareWeightTon = tareWeightTon;
    }

    public BigDecimal getNetWeightTon() {
        return netWeightTon;
    }

    public void setNetWeightTon(BigDecimal netWeightTon) {
        this.netWeightTon = netWeightTon;
    }

    public LocalDateTime getIntakeTime() {
        return intakeTime;
    }

    public void setIntakeTime(LocalDateTime intakeTime) {
        this.intakeTime = intakeTime;
    }

    public String getScaleNo() {
        return scaleNo;
    }

    public void setScaleNo(String scaleNo) {
        this.scaleNo = scaleNo;
    }

    public BigDecimal getCalorificValue() {
        return calorificValue;
    }

    public void setCalorificValue(BigDecimal calorificValue) {
        this.calorificValue = calorificValue;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getUnloadPit() {
        return unloadPit;
    }

    public void setUnloadPit(String unloadPit) {
        this.unloadPit = unloadPit;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
