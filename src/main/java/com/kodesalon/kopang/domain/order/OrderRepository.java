package com.kodesalon.kopang.domain.order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {

	Order register(Order order);

	Optional<Order> findByOrderNo(Long orderNo);

	void updateOrder(Order order);

	List<Order> findExpiredPendingOrders(LocalDateTime cutoffTime);

	List<Order> findExpiredInProgressOrders(LocalDateTime cutoffTime);

	void updateStatusToCancelInBatch(List<Long> expiredNos);
}
