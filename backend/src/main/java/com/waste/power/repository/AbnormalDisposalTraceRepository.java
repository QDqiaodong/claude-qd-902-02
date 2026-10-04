package com.waste.power.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.waste.power.domain.AbnormalDisposalTrace;

/**
 * 处置单操作轨迹数据访问。
 */
public interface AbnormalDisposalTraceRepository extends JpaRepository<AbnormalDisposalTrace, Long> {

    List<AbnormalDisposalTrace> findByDisposalIdOrderByOperateTimeDescIdDesc(Long disposalId);
}
