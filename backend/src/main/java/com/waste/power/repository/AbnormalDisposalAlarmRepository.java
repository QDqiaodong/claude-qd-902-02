package com.waste.power.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.waste.power.domain.AbnormalDisposalAlarm;

/**
 * 处置单-告警证据关联数据访问。
 */
public interface AbnormalDisposalAlarmRepository extends JpaRepository<AbnormalDisposalAlarm, Long> {

    List<AbnormalDisposalAlarm> findByDisposalIdOrderByBindTimeDesc(Long disposalId);

    Optional<AbnormalDisposalAlarm> findByDisposalIdAndAlarmId(Long disposalId, Long alarmId);
}
