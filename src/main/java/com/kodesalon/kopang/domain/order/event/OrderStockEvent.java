package com.kodesalon.kopang.domain.order.event;

import java.util.UUID;

public record OrderStockEvent(
	String id,
	Long orderNo,
	Long productNo,
	Long warehouseNo,
	Integer count
) {
	public static OrderStockEvent create(Long orderNo, Long productNo, Long warehouseNo, Integer count) {
		return new OrderStockEvent(UUID.randomUUID().toString(), orderNo, productNo, warehouseNo, count);
	}
}
