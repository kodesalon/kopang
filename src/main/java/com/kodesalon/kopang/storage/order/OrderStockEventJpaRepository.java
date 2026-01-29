package com.kodesalon.kopang.storage.order;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStockEventJpaRepository extends JpaRepository<OrderStockEventJpaEntity, String> {
	List<OrderStockEventJpaEntity> findAllByPublishedFalseAndCreatedAtBefore(LocalDateTime cutoffTime);
}
