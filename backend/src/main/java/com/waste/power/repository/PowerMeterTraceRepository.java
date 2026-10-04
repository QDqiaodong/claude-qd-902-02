package com.waste.power.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.waste.power.domain.PowerMeterTrace;

/**
 * 发电日结 / 表计处置操作轨迹数据访问。
 */
public interface PowerMeterTraceRepository extends JpaRepository<PowerMeterTrace, Long> {

    List<PowerMeterTrace> findBySettlementIdOrderByOperateTimeDescIdDesc(Long settlementId);

    List<PowerMeterTrace> findTop30ByUnitNoOrderByOperateTimeDescIdDesc(String unitNo);
}
