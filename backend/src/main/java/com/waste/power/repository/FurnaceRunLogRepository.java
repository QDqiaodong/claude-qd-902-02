package com.waste.power.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.waste.power.domain.FurnaceRunLog;
import com.waste.power.dto.FurnaceMonthlyDTO;

/**
 * 启停炉与运行记录数据访问（含按炉号 + 月份的 JPQL 统计）。
 */
public interface FurnaceRunLogRepository extends JpaRepository<FurnaceRunLog, Long> {

    /** 【JPQL 4】按炉号统计月度处理量与发电量 */
    @Query("select new com.waste.power.dto.FurnaceMonthlyDTO(l.furnaceCode, l.periodMonth, sum(l.processTon), "
            + "sum(l.powerKwh), count(l)) from FurnaceRunLog l group by l.furnaceCode, l.periodMonth "
            + "order by l.furnaceCode asc, l.periodMonth asc")
    List<FurnaceMonthlyDTO> monthlyStatsByFurnace();

    List<FurnaceRunLog> findTop30ByOrderByOperateTimeDesc();

    List<FurnaceRunLog> findTop10ByFurnaceCodeOrderByOperateTimeDesc(String furnaceCode);
}
