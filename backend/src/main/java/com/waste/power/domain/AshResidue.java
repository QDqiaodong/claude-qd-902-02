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
 * 灰渣与渗滤液台账：飞灰、炉渣、固化后去向、渗滤液处理量与出水 COD。
 */
@Entity
@Table(name = "ash_residue")
public class AshResidue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_date")
    private LocalDate recordDate;

    /** 飞灰产生量 t */
    @Column(name = "fly_ash_ton", precision = 10, scale = 2)
    private BigDecimal flyAshTon;

    /** 炉渣产生量 t */
    @Column(name = "bottom_ash_ton", precision = 10, scale = 2)
    private BigDecimal bottomAshTon;

    /** 飞灰固化量 t */
    @Column(name = "solidified_ton", precision = 10, scale = 2)
    private BigDecimal solidifiedTon;

    /** 固化后去向 */
    @Column(name = "disposal_dest", length = 64)
    private String disposalDest;

    /** 渗滤液处理站处理量 m³ */
    @Column(name = "leachate_treated_m3", precision = 10, scale = 2)
    private BigDecimal leachateTreatedM3;

    /** 出水 COD mg/L */
    @Column(name = "effluent_cod", precision = 8, scale = 2)
    private BigDecimal effluentCod;

    /** 渗滤液出水是否达标 */
    @Column(name = "effluent_ok")
    private Boolean effluentOk;

    @Column(name = "shift", length = 16)
    private String shift;

    @Column(name = "remark", length = 128)
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
    }

    public BigDecimal getFlyAshTon() {
        return flyAshTon;
    }

    public void setFlyAshTon(BigDecimal flyAshTon) {
        this.flyAshTon = flyAshTon;
    }

    public BigDecimal getBottomAshTon() {
        return bottomAshTon;
    }

    public void setBottomAshTon(BigDecimal bottomAshTon) {
        this.bottomAshTon = bottomAshTon;
    }

    public BigDecimal getSolidifiedTon() {
        return solidifiedTon;
    }

    public void setSolidifiedTon(BigDecimal solidifiedTon) {
        this.solidifiedTon = solidifiedTon;
    }

    public String getDisposalDest() {
        return disposalDest;
    }

    public void setDisposalDest(String disposalDest) {
        this.disposalDest = disposalDest;
    }

    public BigDecimal getLeachateTreatedM3() {
        return leachateTreatedM3;
    }

    public void setLeachateTreatedM3(BigDecimal leachateTreatedM3) {
        this.leachateTreatedM3 = leachateTreatedM3;
    }

    public BigDecimal getEffluentCod() {
        return effluentCod;
    }

    public void setEffluentCod(BigDecimal effluentCod) {
        this.effluentCod = effluentCod;
    }

    public Boolean getEffluentOk() {
        return effluentOk;
    }

    public void setEffluentOk(Boolean effluentOk) {
        this.effluentOk = effluentOk;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
