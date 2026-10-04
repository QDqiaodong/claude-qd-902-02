package com.waste.power.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 处置单-环保告警证据关联：自动触发与人工发起并发到达时，后到一方的证据挂到既有处置单上，
 * 同一条告警不会重复冻结，也不会因「沿用告警已处理标记」而丢失后到严重告警。
 */
@Entity
@Table(name = "abnormal_disposal_alarm")
public class AbnormalDisposalAlarm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "disposal_id", nullable = false)
    private Long disposalId;

    @Column(name = "alarm_id", nullable = false)
    private Long alarmId;

    /** SNAPSHOT 建立时快照 / MERGED 并发后合并 */
    @Column(name = "bind_source", nullable = false, length = 16)
    private String bindSource;

    @Column(name = "bind_time", nullable = false)
    private LocalDateTime bindTime;

    public AbnormalDisposalAlarm() {
    }

    public AbnormalDisposalAlarm(Long disposalId, Long alarmId, String bindSource, LocalDateTime bindTime) {
        this.disposalId = disposalId;
        this.alarmId = alarmId;
        this.bindSource = bindSource;
        this.bindTime = bindTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDisposalId() {
        return disposalId;
    }

    public void setDisposalId(Long disposalId) {
        this.disposalId = disposalId;
    }

    public Long getAlarmId() {
        return alarmId;
    }

    public void setAlarmId(Long alarmId) {
        this.alarmId = alarmId;
    }

    public String getBindSource() {
        return bindSource;
    }

    public void setBindSource(String bindSource) {
        this.bindSource = bindSource;
    }

    public LocalDateTime getBindTime() {
        return bindTime;
    }

    public void setBindTime(LocalDateTime bindTime) {
        this.bindTime = bindTime;
    }
}
