package com.kodesalon.kopang.storage.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.kodesalon.kopang.domain.order.OrderStatus;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {

	OrderJpaEntity findByNo(Long orderNo);

	@Modifying
	@Query("UPDATE OrderJpaEntity o SET o.status = :status WHERE o.no = :orderNo")
	void updateOrder(Long orderNo, OrderStatus status);

	@Modifying
	@Query("UPDATE OrderJpaEntity o SET o.status = :status WHERE o.no = :orderNo AND o.status = 'PENDING'")
	int updateStatusToInProgress(Long orderNo, OrderStatus status);

	@Modifying
	@Query("UPDATE OrderJpaEntity o SET o.status = :status WHERE o.no = :orderNo AND o.status = 'PAYMENT_IN_PROGRESS'")
	int updateStatusToPending(Long orderNo, OrderStatus status);

	@Modifying
	@Query("UPDATE OrderJpaEntity o SET o.status = :status WHERE o.no = :orderNo AND o.status <> 'CANCELLED'")
	int updateStatusCancel(Long orderNo, OrderStatus status);
}
