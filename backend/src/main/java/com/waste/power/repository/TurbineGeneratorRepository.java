package com.waste.power.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.waste.power.domain.TurbineGenerator;

/**
 * 汽轮发电机组数据访问。
 */
public interface TurbineGeneratorRepository extends JpaRepository<TurbineGenerator, Long> {

    Optional<TurbineGenerator> findByUnitNo(String unitNo);

    /** 行锁加载机组：同机组日结/校表串行化，保证相邻表码口径与并发请求严格互斥 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TurbineGenerator t where t.unitNo = :unitNo")
    Optional<TurbineGenerator> lockByUnitNo(@Param("unitNo") String unitNo);
}
