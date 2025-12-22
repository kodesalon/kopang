package com.kodesalon.kopang.storage.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface StockJpaRepository extends JpaRepository<StockJpaEntity, Long> {

	@Query("SELECT s FROM StockJpaEntity s WHERE s.productNo = :productNo")
	StockJpaEntity findByProductNoWithLock(Long productNo);

	@Modifying
	@Query("UPDATE StockJpaEntity s SET s.quantity = :quantity WHERE s.productNo = :productNo")
	void updateStock(Long productNo, Integer quantity);
}
