package com.waste.power.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 环保排放仪表盘聚合视图（六项指标仪表 + 告警 + 灰渣台账 + 超标排行）。
 */
public class DashboardVO {

    private List<EmissionPayload.VO> gauges;
    private List<EmissionPayload.AlarmVO> alarms;
    private List<AshPayload.VO> ashRecords;
    private List<EmissionExceedRankDTO> rank;
    private Long exceedCount;
    private Long alarmCount;
    private BigDecimal flyAshTotalTon;
    private BigDecimal bottomAshTotalTon;
    private BigDecimal leachateTotalM3;
    private Boolean cached;

    public List<EmissionPayload.VO> getGauges() {
        return gauges;
    }

    public void setGauges(List<EmissionPayload.VO> gauges) {
        this.gauges = gauges;
    }

    public List<EmissionPayload.AlarmVO> getAlarms() {
        return alarms;
    }

    public void setAlarms(List<EmissionPayload.AlarmVO> alarms) {
        this.alarms = alarms;
    }

    public List<AshPayload.VO> getAshRecords() {
        return ashRecords;
    }

    public void setAshRecords(List<AshPayload.VO> ashRecords) {
        this.ashRecords = ashRecords;
    }

    public List<EmissionExceedRankDTO> getRank() {
        return rank;
    }

    public void setRank(List<EmissionExceedRankDTO> rank) {
        this.rank = rank;
    }

    public Long getExceedCount() {
        return exceedCount;
    }

    public void setExceedCount(Long exceedCount) {
        this.exceedCount = exceedCount;
    }

    public Long getAlarmCount() {
        return alarmCount;
    }

    public void setAlarmCount(Long alarmCount) {
        this.alarmCount = alarmCount;
    }

    public BigDecimal getFlyAshTotalTon() {
        return flyAshTotalTon;
    }

    public void setFlyAshTotalTon(BigDecimal flyAshTotalTon) {
        this.flyAshTotalTon = flyAshTotalTon;
    }

    public BigDecimal getBottomAshTotalTon() {
        return bottomAshTotalTon;
    }

    public void setBottomAshTotalTon(BigDecimal bottomAshTotalTon) {
        this.bottomAshTotalTon = bottomAshTotalTon;
    }

    public BigDecimal getLeachateTotalM3() {
        return leachateTotalM3;
    }

    public void setLeachateTotalM3(BigDecimal leachateTotalM3) {
        this.leachateTotalM3 = leachateTotalM3;
    }

    public Boolean getCached() {
        return cached;
    }

    public void setCached(Boolean cached) {
        this.cached = cached;
    }
}
