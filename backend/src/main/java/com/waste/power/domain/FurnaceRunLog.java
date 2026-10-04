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
 * 焚烧炉启停炉与运行记录：炉号、动作（启炉/稳燃/停炉）、操作时间、炉膛温度、
 * 当班处理量、当班发电量、运行小时、操作员。
 */
@Entity
@Table(name = "furnace_run_log")
public class FurnaceRunLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "furnace_code", nullable = false, length = 32)
    private String furnaceCode;

    /** 动作：START / STABILIZE / STOP */
    @Column(name = "action", nullable = false, length = 16)
    private String action;

    @Column(name = "operate_time")
    private LocalDateTime operateTime;

    /** 所属月份 yyyy-MM，用于月度台账统计 */
    @Column(name = "period_month", length = 7)
    private String periodMonth;

    @Column(name = "furnace_temp_c", precision = 8, scale = 1)
    private BigDecimal furnaceTempC;

    /** 当班处理量 t */
    @Column(name = "process_ton", precision = 10, scale = 2)
    private BigDecimal processTon;

    /** 当班发电量 kWh */
    @Column(name = "power_kwh", precision = 14, scale = 2)
    private BigDecimal powerKwh;

    /** 当班运行小时 h */
    @Column(name = "run_hours", precision = 8, scale = 1)
    private BigDecimal runHours;

    @Column(name = "operator", length = 32)
    private String operator;

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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public LocalDateTime getOperateTime() {
        return operateTime;
    }

    public void setOperateTime(LocalDateTime operateTime) {
        this.operateTime = operateTime;
    }

    public String getPeriodMonth() {
        return periodMonth;
    }

    public void setPeriodMonth(String periodMonth) {
        this.periodMonth = periodMonth;
    }

    public BigDecimal getFurnaceTempC() {
        return furnaceTempC;
    }

    public void setFurnaceTempC(BigDecimal furnaceTempC) {
        this.furnaceTempC = furnaceTempC;
    }

    public BigDecimal getProcessTon() {
        return processTon;
    }

    public void setProcessTon(BigDecimal processTon) {
        this.processTon = processTon;
    }

    public BigDecimal getPowerKwh() {
        return powerKwh;
    }

    public void setPowerKwh(BigDecimal powerKwh) {
        this.powerKwh = powerKwh;
    }

    public BigDecimal getRunHours() {
        return runHours;
    }

    public void setRunHours(BigDecimal runHours) {
        this.runHours = runHours;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
