package com.waste.power.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.DiscriminatorFormula;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * 焚烧炉抽象父类（单表继承 SINGLE_TABLE）。
 *
 * 三种子类（炉排炉 / 流化床 / 回转窑）共用一张 incinerator 表，
 * 子类判别【不用】@DiscriminatorColumn 字符串列，而是用 Hibernate 的 @DiscriminatorFormula：
 * 由 SQL CASE WHEN 表达式（优先按炉型代码、并可按额定处理量兜底推导）算出子类标识。
 */
@Entity
@Table(name = "incinerator")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula("CASE "
        + "WHEN furnace_type_code = 'GRATE' THEN 'GRATE' "
        + "WHEN furnace_type_code = 'FLUIDIZED' THEN 'FLUIDIZED' "
        + "WHEN furnace_type_code = 'ROTARY' THEN 'ROTARY' "
        + "WHEN rated_capacity_tpd >= 700 THEN 'GRATE' "
        + "WHEN rated_capacity_tpd >= 350 THEN 'FLUIDIZED' "
        + "ELSE 'ROTARY' END")
public abstract class Incinerator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "furnace_code", nullable = false, unique = true, length = 32)
    private String furnaceCode;

    @Column(name = "furnace_name", length = 64)
    private String furnaceName;

    /** 炉型代码：GRATE / FLUIDIZED / ROTARY（同时作为判别公式的输入） */
    @Column(name = "furnace_type_code", length = 16)
    private String furnaceTypeCode;

    /** 额定处理量 t/d */
    @Column(name = "rated_capacity_tpd", precision = 10, scale = 2)
    private BigDecimal ratedCapacityTpd;

    /** 炉膛温度 ℃ */
    @Column(name = "furnace_temp_c", precision = 8, scale = 1)
    private BigDecimal furnaceTempC;

    /** 烟气停留时间 s */
    @Column(name = "flue_gas_residence_s", precision = 6, scale = 2)
    private BigDecimal flueGasResidenceS;

    /** CO 浓度 ppm */
    @Column(name = "co_ppm", precision = 8, scale = 2)
    private BigDecimal coPpm;

    /** 燃烧状态：STARTING/STABLE/BANKING/SHUTDOWN/FAULT */
    @Column(name = "combustion_status", length = 16)
    private String combustionStatus;

    /** 当日入炉量 t */
    @Column(name = "daily_process_ton", precision = 10, scale = 2)
    private BigDecimal dailyProcessTon;

    /** 累计运行小时 h */
    @Column(name = "running_hours", precision = 10, scale = 1)
    private BigDecimal runningHours;

    @Column(name = "last_start_time")
    private LocalDateTime lastStartTime;

    /** 供料抓斗编号 */
    @Column(name = "grab_no", length = 16)
    private String grabNo;

    /** 对应排放口编号：同炉严重排放告警据此归集到处置单 */
    @Column(name = "stack_code", length = 32)
    private String stackCode;

    @Column(name = "remark", length = 128)
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFurnaceCode() {
        return furnaceCode;
    }

    public void setFurnaceCode(String furnaceCode) {
        this.furnaceCode = furnaceCode;
    }

    public String getFurnaceName() {
        return furnaceName;
    }

    public void setFurnaceName(String furnaceName) {
        this.furnaceName = furnaceName;
    }

    public String getFurnaceTypeCode() {
        return furnaceTypeCode;
    }

    public void setFurnaceTypeCode(String furnaceTypeCode) {
        this.furnaceTypeCode = furnaceTypeCode;
    }

    public BigDecimal getRatedCapacityTpd() {
        return ratedCapacityTpd;
    }

    public void setRatedCapacityTpd(BigDecimal ratedCapacityTpd) {
        this.ratedCapacityTpd = ratedCapacityTpd;
    }

    public BigDecimal getFurnaceTempC() {
        return furnaceTempC;
    }

    public void setFurnaceTempC(BigDecimal furnaceTempC) {
        this.furnaceTempC = furnaceTempC;
    }

    public BigDecimal getFlueGasResidenceS() {
        return flueGasResidenceS;
    }

    public void setFlueGasResidenceS(BigDecimal flueGasResidenceS) {
        this.flueGasResidenceS = flueGasResidenceS;
    }

    public BigDecimal getCoPpm() {
        return coPpm;
    }

    public void setCoPpm(BigDecimal coPpm) {
        this.coPpm = coPpm;
    }

    public String getCombustionStatus() {
        return combustionStatus;
    }

    public void setCombustionStatus(String combustionStatus) {
        this.combustionStatus = combustionStatus;
    }

    public BigDecimal getDailyProcessTon() {
        return dailyProcessTon;
    }

    public void setDailyProcessTon(BigDecimal dailyProcessTon) {
        this.dailyProcessTon = dailyProcessTon;
    }

    public BigDecimal getRunningHours() {
        return runningHours;
    }

    public void setRunningHours(BigDecimal runningHours) {
        this.runningHours = runningHours;
    }

    public LocalDateTime getLastStartTime() {
        return lastStartTime;
    }

    public void setLastStartTime(LocalDateTime lastStartTime) {
        this.lastStartTime = lastStartTime;
    }

    public String getGrabNo() {
        return grabNo;
    }

    public void setGrabNo(String grabNo) {
        this.grabNo = grabNo;
    }

    public String getStackCode() {
        return stackCode;
    }

    public void setStackCode(String stackCode) {
        this.stackCode = stackCode;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
