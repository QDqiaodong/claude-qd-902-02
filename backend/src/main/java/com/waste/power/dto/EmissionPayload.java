package com.waste.power.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.waste.power.common.Dict;
import com.waste.power.domain.EmissionRecord;
import com.waste.power.domain.EnvAlarm;

/**
 * 环保排放与环保告警的请求与视图对象。
 */
public class EmissionPayload {

    /** 排放登记请求 */
    public static class Request {

        private String indicatorCode;
        private BigDecimal measuredValue;
        private String periodLabel;
        private LocalDateTime monitorTime;
        private String stackCode;
        private String emissionFlag;
        private String remark;

        public String getIndicatorCode() {
            return indicatorCode;
        }

        public void setIndicatorCode(String indicatorCode) {
            this.indicatorCode = indicatorCode;
        }

        public BigDecimal getMeasuredValue() {
            return measuredValue;
        }

        public void setMeasuredValue(BigDecimal measuredValue) {
            this.measuredValue = measuredValue;
        }

        public String getPeriodLabel() {
            return periodLabel;
        }

        public void setPeriodLabel(String periodLabel) {
            this.periodLabel = periodLabel;
        }

        public LocalDateTime getMonitorTime() {
            return monitorTime;
        }

        public void setMonitorTime(LocalDateTime monitorTime) {
            this.monitorTime = monitorTime;
        }

        public String getStackCode() {
            return stackCode;
        }

        public void setStackCode(String stackCode) {
            this.stackCode = stackCode;
        }

        public String getEmissionFlag() {
            return emissionFlag;
        }

        public void setEmissionFlag(String emissionFlag) {
            this.emissionFlag = emissionFlag;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }

    /** 排放记录视图（含占标率，供半圆仪表定位指针） */
    public static class VO {

        private Long id;
        private String indicatorCode;
        private String indicatorName;
        private BigDecimal measuredValue;
        private BigDecimal limitValue;
        private String unit;
        private Boolean exceeded;
        private BigDecimal exceedRatio;
        private String periodLabel;
        private LocalDateTime monitorTime;
        private String stackCode;
        private String emissionFlag;
        private String emissionFlagName;
        private String remark;
        /** 实测值 / 限值（0~1+），指针摆位用 */
        private Double gaugeRatio;

        public static VO from(EmissionRecord record) {
            VO vo = new VO();
            vo.id = record.getId();
            vo.indicatorCode = record.getIndicatorCode();
            vo.indicatorName = record.getIndicatorName();
            vo.measuredValue = record.getMeasuredValue();
            vo.limitValue = record.getLimitValue();
            vo.unit = record.getUnit();
            vo.exceeded = record.getExceeded();
            vo.exceedRatio = record.getExceedRatio();
            vo.periodLabel = record.getPeriodLabel();
            vo.monitorTime = record.getMonitorTime();
            vo.stackCode = record.getStackCode();
            vo.emissionFlag = record.getEmissionFlag();
            vo.emissionFlagName = Dict.emissionFlagName(record.getEmissionFlag());
            vo.remark = record.getRemark();
            if (record.getLimitValue() != null && record.getMeasuredValue() != null
                    && record.getLimitValue().compareTo(BigDecimal.ZERO) > 0) {
                vo.gaugeRatio = record.getMeasuredValue().doubleValue() / record.getLimitValue().doubleValue();
            } else {
                vo.gaugeRatio = 0D;
            }
            return vo;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getIndicatorCode() {
            return indicatorCode;
        }

        public void setIndicatorCode(String indicatorCode) {
            this.indicatorCode = indicatorCode;
        }

        public String getIndicatorName() {
            return indicatorName;
        }

        public void setIndicatorName(String indicatorName) {
            this.indicatorName = indicatorName;
        }

        public BigDecimal getMeasuredValue() {
            return measuredValue;
        }

        public void setMeasuredValue(BigDecimal measuredValue) {
            this.measuredValue = measuredValue;
        }

        public BigDecimal getLimitValue() {
            return limitValue;
        }

        public void setLimitValue(BigDecimal limitValue) {
            this.limitValue = limitValue;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public Boolean getExceeded() {
            return exceeded;
        }

        public void setExceeded(Boolean exceeded) {
            this.exceeded = exceeded;
        }

        public BigDecimal getExceedRatio() {
            return exceedRatio;
        }

        public void setExceedRatio(BigDecimal exceedRatio) {
            this.exceedRatio = exceedRatio;
        }

        public String getPeriodLabel() {
            return periodLabel;
        }

        public void setPeriodLabel(String periodLabel) {
            this.periodLabel = periodLabel;
        }

        public LocalDateTime getMonitorTime() {
            return monitorTime;
        }

        public void setMonitorTime(LocalDateTime monitorTime) {
            this.monitorTime = monitorTime;
        }

        public String getStackCode() {
            return stackCode;
        }

        public void setStackCode(String stackCode) {
            this.stackCode = stackCode;
        }

        public String getEmissionFlag() {
            return emissionFlag;
        }

        public void setEmissionFlag(String emissionFlag) {
            this.emissionFlag = emissionFlag;
        }

        public String getEmissionFlagName() {
            return emissionFlagName;
        }

        public void setEmissionFlagName(String emissionFlagName) {
            this.emissionFlagName = emissionFlagName;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public Double getGaugeRatio() {
            return gaugeRatio;
        }

        public void setGaugeRatio(Double gaugeRatio) {
            this.gaugeRatio = gaugeRatio;
        }
    }

    /** 环保告警视图 */
    public static class AlarmVO {

        private Long id;
        private String alarmNo;
        private String indicatorCode;
        private String indicatorName;
        private BigDecimal measuredValue;
        private BigDecimal limitValue;
        private BigDecimal exceedRatio;
        private String alarmLevel;
        private String alarmLevelName;
        private String periodLabel;
        private String stackCode;
        private LocalDateTime alarmTime;
        private Boolean handled;
        private String handleNote;

        public static AlarmVO from(EnvAlarm alarm) {
            AlarmVO vo = new AlarmVO();
            vo.id = alarm.getId();
            vo.alarmNo = alarm.getAlarmNo();
            vo.indicatorCode = alarm.getIndicatorCode();
            vo.indicatorName = alarm.getIndicatorName();
            vo.measuredValue = alarm.getMeasuredValue();
            vo.limitValue = alarm.getLimitValue();
            vo.exceedRatio = alarm.getExceedRatio();
            vo.alarmLevel = alarm.getAlarmLevel();
            vo.alarmLevelName = Dict.alarmLevelName(alarm.getAlarmLevel());
            vo.periodLabel = alarm.getPeriodLabel();
            vo.stackCode = alarm.getStackCode();
            vo.alarmTime = alarm.getAlarmTime();
            vo.handled = alarm.getHandled();
            vo.handleNote = alarm.getHandleNote();
            return vo;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getAlarmNo() {
            return alarmNo;
        }

        public void setAlarmNo(String alarmNo) {
            this.alarmNo = alarmNo;
        }

        public String getIndicatorCode() {
            return indicatorCode;
        }

        public void setIndicatorCode(String indicatorCode) {
            this.indicatorCode = indicatorCode;
        }

        public String getIndicatorName() {
            return indicatorName;
        }

        public void setIndicatorName(String indicatorName) {
            this.indicatorName = indicatorName;
        }

        public BigDecimal getMeasuredValue() {
            return measuredValue;
        }

        public void setMeasuredValue(BigDecimal measuredValue) {
            this.measuredValue = measuredValue;
        }

        public BigDecimal getLimitValue() {
            return limitValue;
        }

        public void setLimitValue(BigDecimal limitValue) {
            this.limitValue = limitValue;
        }

        public BigDecimal getExceedRatio() {
            return exceedRatio;
        }

        public void setExceedRatio(BigDecimal exceedRatio) {
            this.exceedRatio = exceedRatio;
        }

        public String getAlarmLevel() {
            return alarmLevel;
        }

        public void setAlarmLevel(String alarmLevel) {
            this.alarmLevel = alarmLevel;
        }

        public String getAlarmLevelName() {
            return alarmLevelName;
        }

        public void setAlarmLevelName(String alarmLevelName) {
            this.alarmLevelName = alarmLevelName;
        }

        public String getPeriodLabel() {
            return periodLabel;
        }

        public void setPeriodLabel(String periodLabel) {
            this.periodLabel = periodLabel;
        }

        public String getStackCode() {
            return stackCode;
        }

        public void setStackCode(String stackCode) {
            this.stackCode = stackCode;
        }

        public LocalDateTime getAlarmTime() {
            return alarmTime;
        }

        public void setAlarmTime(LocalDateTime alarmTime) {
            this.alarmTime = alarmTime;
        }

        public Boolean getHandled() {
            return handled;
        }

        public void setHandled(Boolean handled) {
            this.handled = handled;
        }

        public String getHandleNote() {
            return handleNote;
        }

        public void setHandleNote(String handleNote) {
            this.handleNote = handleNote;
        }
    }

    /** 告警处理请求 */
    public static class HandleRequest {

        private String handleNote;

        public String getHandleNote() {
            return handleNote;
        }

        public void setHandleNote(String handleNote) {
            this.handleNote = handleNote;
        }
    }
}
