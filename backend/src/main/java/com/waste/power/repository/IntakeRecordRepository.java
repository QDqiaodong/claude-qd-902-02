package com.waste.power.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.waste.power.domain.IntakeRecord;
import com.waste.power.dto.CategoryShareDTO;
import com.waste.power.dto.DailyIntakeStatDTO;
import com.waste.power.dto.SourceSummaryDTO;

/**
 * 进厂车次与地磅计量数据访问（含 3 处手写 JPQL 汇总查询）。
 */
public interface IntakeRecordRepository extends JpaRepository<IntakeRecord, Long> {

    /** 【JPQL 1】按来源行政区划汇总进厂车次、净重与平均热值 */
    @Query("select new com.waste.power.dto.SourceSummaryDTO(i.sourceRegion, count(i), sum(i.netWeightTon), "
            + "avg(i.calorificValue)) from IntakeRecord i group by i.sourceRegion order by sum(i.netWeightTon) desc")
    List<SourceSummaryDTO> summaryByRegion();

    /** 【JPQL 2】指定时段内的进厂汇总：车次、净重合计、平均净重 */
    @Query("select new com.waste.power.dto.DailyIntakeStatDTO(count(i), sum(i.netWeightTon), avg(i.netWeightTon)) "
            + "from IntakeRecord i where i.intakeTime >= :start and i.intakeTime < :end")
    DailyIntakeStatDTO statBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 【JPQL 3】按垃圾类别统计净重与车次（页面环形图） */
    @Query("select new com.waste.power.dto.CategoryShareDTO(i.wasteCategory, sum(i.netWeightTon), count(i)) "
            + "from IntakeRecord i where i.intakeTime >= :start and i.intakeTime < :end group by i.wasteCategory")
    List<CategoryShareDTO> categoryShares(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<IntakeRecord> findTop50ByOrderByIntakeTimeDesc();
}
