package com.kodesalon.kopang.storage.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface StockJpaRepository extends JpaRepository<StockJpaEntity, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM StockJpaEntity s WHERE s.productNo = :productNo")
	StockJpaEntity findByProductNoWithLock(Long productNo);

	@Modifying
	@Query("UPDATE StockJpaEntity s SET s.quantity = :quantity WHERE s.productNo = :productNo")
	void updateStock(Long productNo, Integer quantity);
}
