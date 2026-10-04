package com.waste.power.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.waste.power.domain.EnvAlarm;

/**
 * 环保告警数据访问。
 */
public interface EnvAlarmRepository extends JpaRepository<EnvAlarm, Long> {

    List<EnvAlarm> findTop30ByOrderByAlarmTimeDesc();

    List<EnvAlarm> findTop20ByHandledFalseOrderByAlarmTimeDesc();

    Optional<EnvAlarm> findByAlarmNo(String alarmNo);

    /** 同炉未解除严重告警（冻结时点之后）：处置单证据快照、复核恢复判定同一口径，handled=true 即解除联锁 */
    List<EnvAlarm> findByStackCodeAndAlarmLevelAndHandledFalseAndAlarmTimeGreaterThanEqualOrderByAlarmTimeDesc(
            String stackCode, String alarmLevel, LocalDateTime since);

    /** 复核窗口判定：在持有焚烧炉行锁的事务内调用即可串行化；不对零行结果集加锁，避免 MySQL 间隙锁死锁 */
    @Query("select a from EnvAlarm a where a.stackCode = :stackCode and a.alarmLevel = 'SEVERE' "
            + "and a.handled = false and a.alarmTime >= :since order by a.alarmTime desc")
    List<EnvAlarm> lockActiveSevere(@Param("stackCode") String stackCode,
            @Param("since") LocalDateTime since);
}
