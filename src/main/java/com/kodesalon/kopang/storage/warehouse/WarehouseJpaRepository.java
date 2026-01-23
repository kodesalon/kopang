package com.kodesalon.kopang.storage.warehouse;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WarehouseJpaRepository extends JpaRepository<WarehouseJpaEntity, Long> {

	@Query("""
		SELECT w FROM WarehouseJpaEntity w
		WHERE w.no IN (SELECT s.warehouseNo FROM StockJpaEntity s WHERE s.productNo = :productNo)""")
	List<WarehouseJpaEntity> findAllByProductNo(Long productNo);
}
