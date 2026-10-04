package com.waste.power.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.waste.power.domain.FluidizedBed;
import com.waste.power.domain.GrateFurnace;
import com.waste.power.domain.Incinerator;

/**
 * 焚烧炉数据访问：单表继承 + @DiscriminatorFormula，子类由公式判别。
 *
 * 这里给出两条体现继承体系的查询：
 *   1) TYPE() 按子类分组统计；
 *   2) treat() 在 WHERE 中访问子类专属字段（炉排速度）。
 */
public interface IncineratorRepository extends JpaRepository<Incinerator, Long> {

    /** 【TYPE()】按焚烧炉子类（炉排炉/流化床/回转窑）统计台数、当日入炉量、平均炉膛温度、额定处理能力 */
    @Query("select type(i), count(i), sum(i.dailyProcessTon), avg(i.furnaceTempC), sum(i.ratedCapacityTpd) "
            + "from Incinerator i group by type(i)")
    List<Object[]> statsByType();

    /** 【treat()】访问子类 GrateFurnace 的专属字段：炉排速度不低于给定值的炉排炉 */
    @Query("select i from Incinerator i where treat(i as GrateFurnace).grateSpeedMh >= :speed")
    List<Incinerator> findFastGrateFurnaces(@Param("speed") BigDecimal speed);

    /** 【treat()】访问子类 FluidizedBed 的专属字段：流化风速不低于给定值的流化床 */
    @Query("select i from Incinerator i where treat(i as FluidizedBed).fluidizingAirSpeedMs >= :speed")
    List<Incinerator> findHighSpeedFluidizedBeds(@Param("speed") BigDecimal speed);

    Optional<Incinerator> findByFurnaceCode(String furnaceCode);

    /** 行锁加载焚烧炉：建单/合并证据/投料/复核恢复共用，保证同炉联锁动作严格串行 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Incinerator i where i.furnaceCode = :furnaceCode")
    Optional<Incinerator> lockByFurnaceCode(@Param("furnaceCode") String furnaceCode);

    Optional<Incinerator> findFirstByStackCode(String stackCode);
}
