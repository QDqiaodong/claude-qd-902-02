package com.waste.power.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * 回转窑焚烧炉：专属参数为窑体转速、窑体倾角、窑长、窑尾温度。
 */
@Entity
@DiscriminatorValue("ROTARY")
public class RotaryKiln extends Incinerator {

    /** 窑体转速 r/min */
    @Column(name = "kiln_speed_rpm", precision = 6, scale = 2)
    private BigDecimal kilnSpeedRpm;

    /** 窑体倾角 ° */
    @Column(name = "kiln_inclination_deg", precision = 5, scale = 2)
    private BigDecimal kilnInclinationDeg;

    /** 窑长 m */
    @Column(name = "kiln_length_m", precision = 6, scale = 2)
    private BigDecimal kilnLengthM;

    /** 窑尾温度 ℃ */
    @Column(name = "kiln_tail_temp_c", precision = 6, scale = 1)
    private BigDecimal kilnTailTempC;

    public BigDecimal getKilnSpeedRpm() {
        return kilnSpeedRpm;
    }

    public void setKilnSpeedRpm(BigDecimal kilnSpeedRpm) {
        this.kilnSpeedRpm = kilnSpeedRpm;
    }

    public BigDecimal getKilnInclinationDeg() {
        return kilnInclinationDeg;
    }

    public void setKilnInclinationDeg(BigDecimal kilnInclinationDeg) {
        this.kilnInclinationDeg = kilnInclinationDeg;
    }

    public BigDecimal getKilnLengthM() {
        return kilnLengthM;
    }

    public void setKilnLengthM(BigDecimal kilnLengthM) {
        this.kilnLengthM = kilnLengthM;
    }

    public BigDecimal getKilnTailTempC() {
        return kilnTailTempC;
    }

    public void setKilnTailTempC(BigDecimal kilnTailTempC) {
        this.kilnTailTempC = kilnTailTempC;
    }
}
