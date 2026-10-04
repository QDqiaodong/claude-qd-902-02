package com.waste.power.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.waste.power.domain.StoragePit;

/**
 * 垃圾储坑数据访问。
 */
public interface StoragePitRepository extends JpaRepository<StoragePit, Long> {

    Optional<StoragePit> findByPitNo(String pitNo);

    /** 处置单建立时按炉侧供料抓斗反查最近供料坑位，做供料抓斗快照 */
    Optional<StoragePit> findFirstByGrabNo(String grabNo);
}
