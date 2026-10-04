package com.waste.power.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 垃圾储坑：坑位编号、容量、当前库存、堆酵天数、渗滤液抽出量、坑温、投料抓斗、状态。
 */
@Entity
@Table(name = "storage_pit")
public class StoragePit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pit_no", nullable = false, unique = true, length = 16)
    private String pitNo;

    @Column(name = "pit_name", length = 64)
    private String pitName;

    /** 坑容 t */
    @Column(name = "capacity_ton", precision = 10, scale = 2)
    private BigDecimal capacityTon;

    /** 当前库存 t */
    @Column(name = "current_stock_ton", precision = 10, scale = 2)
    private BigDecimal currentStockTon;

    /** 堆酵天数 d */
    @Column(name = "fermentation_days", precision = 5, scale = 1)
    private BigDecimal fermentationDays;

    /** 渗滤液累积抽出量 m³ */
    @Column(name = "leachate_pumped_m3", precision = 10, scale = 2)
    private BigDecimal leachatePumpedM3;

    /** 坑温 ℃ */
    @Column(name = "pit_temp_c", precision = 6, scale = 1)
    private BigDecimal pitTempC;

    /** 投料抓斗编号 */
    @Column(name = "grab_no", length = 16)
    private String grabNo;

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

    public String getPitNo() {
        return pitNo;
    }

    public void setPitNo(String pitNo) {
        this.pitNo = pitNo;
    }

    public String getPitName() {
        return pitName;
    }

    public void setPitName(String pitName) {
        this.pitName = pitName;
    }

    public BigDecimal getCapacityTon() {
        return capacityTon;
    }

    public void setCapacityTon(BigDecimal capacityTon) {
        this.capacityTon = capacityTon;
    }

    public BigDecimal getCurrentStockTon() {
        return currentStockTon;
    }

    public void setCurrentStockTon(BigDecimal currentStockTon) {
        this.currentStockTon = currentStockTon;
    }

    public BigDecimal getFermentationDays() {
        return fermentationDays;
    }

    public void setFermentationDays(BigDecimal fermentationDays) {
        this.fermentationDays = fermentationDays;
    }

    public BigDecimal getLeachatePumpedM3() {
        return leachatePumpedM3;
    }

    public void setLeachatePumpedM3(BigDecimal leachatePumpedM3) {
        this.leachatePumpedM3 = leachatePumpedM3;
    }

    public BigDecimal getPitTempC() {
        return pitTempC;
    }

    public void setPitTempC(BigDecimal pitTempC) {
        this.pitTempC = pitTempC;
    }

    public String getGrabNo() {
        return grabNo;
    }

    public void setGrabNo(String grabNo) {
        this.grabNo = grabNo;
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
