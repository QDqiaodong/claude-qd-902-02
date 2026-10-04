package com.waste.power.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.waste.power.domain.AshResidue;

/**
 * 灰渣与渗滤液台账数据访问。
 */
public interface AshResidueRepository extends JpaRepository<AshResidue, Long> {

    Optional<AshResidue> findFirstByRecordDateAndShift(LocalDate recordDate, String shift);
}
