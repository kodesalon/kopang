package com.kodesalon.kopang.domain.order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {

	Order register(Order order);

	Optional<Order> findByOrderNo(Long orderNo);

	/**
	 * 주문 상태가 expected 일 때만 next 로 바꾼다.
	 * 조회한 뒤 다른 요청(중복 결제 요청, 만료 취소 스케줄러)이 먼저 상태를 바꿨으면 바꾸지 않고 false 를 돌려준다.
	 */
	boolean updateStatus(Long orderNo, OrderStatus expected, OrderStatus next);

	List<Order> findExpiredPendingOrders(LocalDateTime cutoffTime);

	List<Order> findExpiredInProgressOrders(LocalDateTime cutoffTime);
}
