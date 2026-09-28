package com.kodesalon.kopang.service.exception;

import com.kodesalon.kopang.domain.order.OrderStatus;

public class OrderStatusConflictException extends RuntimeException {

	public OrderStatusConflictException(String message) {
		super(message);
	}

	public static OrderStatusConflictException of(Long orderNo, OrderStatus expected, OrderStatus next) {
		return new OrderStatusConflictException(String.format(
			"주문 %d 의 상태가 이미 바뀌어 %s → %s 로 변경하지 못했습니다.", orderNo, expected, next));
	}
}
