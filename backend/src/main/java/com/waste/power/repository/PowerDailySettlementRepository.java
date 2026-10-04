package com.waste.power.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.waste.power.domain.PowerDailySettlement;

/**
 * 发电日结数据访问：机组 + 统计日唯一（uk_unit_stat_date 库级兜底重复请求）。
 * 机组行锁已把同机组写入串行化，这里的查询不再加 FOR UPDATE，避免 MySQL 间隙锁互锁。
 */
public interface PowerDailySettlementRepository extends JpaRepository<PowerDailySettlement, Long> {

    Optional<PowerDailySettlement> findByUnitNoAndStatDate(String unitNo, LocalDate statDate);

    List<PowerDailySettlement> findByUnitNoOrderByStatDateDesc(String unitNo);

    List<PowerDailySettlement> findByUnitNoAndStatDateLessThanOrderByStatDateDesc(String unitNo, LocalDate statDate);

    List<PowerDailySettlement> findTop80ByOrderByStatDateDescUpdateTimeDesc();
}
