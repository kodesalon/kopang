package com.kodesalon.kopang.api.controller.v1.order;

import java.math.BigDecimal;

import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.domain.order.OrderStatus;
import com.kodesalon.kopang.domain.stock.Stock;

public record ReservationOrderResponse(
	Long orderNo,
	BigDecimal totalPrice,
	Integer quantity,
	OrderStatus orderStatus
) {
	public static ReservationOrderResponse of(Order order, Stock stock) {
		return new ReservationOrderResponse(
			order.getNo(),
			order.getTotalPrice().getAmount(),
			stock.getQuantity(),
			order.getStatus()
		);
	}
}
