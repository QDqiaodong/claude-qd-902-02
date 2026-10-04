package com.waste.power.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.waste.power.domain.Boiler;

/**
 * 余热锅炉数据访问。
 */
public interface BoilerRepository extends JpaRepository<Boiler, Long> {

    Optional<Boiler> findByBoilerNo(String boilerNo);
}
