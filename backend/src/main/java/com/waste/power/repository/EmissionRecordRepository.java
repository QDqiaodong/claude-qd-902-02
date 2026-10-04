package com.waste.power.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.waste.power.domain.EmissionRecord;
import com.waste.power.dto.EmissionExceedRankDTO;

/**
 * 烟气排放指标数据访问（含超标次数排行 JPQL）。
 */
public interface EmissionRecordRepository extends JpaRepository<EmissionRecord, Long> {

    /** 【JPQL 5】排放指标超标次数排行（超标次数、峰值、限值） */
    @Query("select new com.waste.power.dto.EmissionExceedRankDTO(e.indicatorCode, count(e), max(e.measuredValue), "
            + "min(e.limitValue)) from EmissionRecord e where e.exceeded = true group by e.indicatorCode "
            + "order by count(e) desc")
    List<EmissionExceedRankDTO> exceedRank();

    List<EmissionRecord> findTop80ByOrderByMonitorTimeDesc();

    List<EmissionRecord> findByIndicatorCodeOrderByMonitorTimeDesc(String indicatorCode);
}
