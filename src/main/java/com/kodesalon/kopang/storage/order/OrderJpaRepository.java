package com.kodesalon.kopang.storage.order;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.kodesalon.kopang.domain.order.OrderStatus;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {

	OrderJpaEntity findByNo(Long orderNo);

	@Modifying
	@Query("UPDATE OrderJpaEntity o SET o.status = :status WHERE o.no = :orderNo")
	void updateOrder(Long orderNo, OrderStatus status);

	@Query("""
		SELECT o FROM OrderJpaEntity o JOIN FETCH o.orderProducts
		WHERE o.status = :status AND o.orderedAt < :cutoffTime""")
	List<OrderJpaEntity> findExpiredOrders(OrderStatus status, LocalDateTime cutoffTime, Pageable pageable);

	@Modifying
	@Query("UPDATE OrderJpaEntity o SET o.status = :canceled WHERE o.no IN :expiredNos AND o.status IN :statuses")
	void updateStatusToCancelInBatch(List<Long> expiredNos, OrderStatus canceled, List<OrderStatus> statuses);
}
