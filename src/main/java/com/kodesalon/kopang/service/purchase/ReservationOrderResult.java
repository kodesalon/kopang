package com.kodesalon.kopang.service.purchase;

import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.domain.stock.StockQuantity;

public record ReservationOrderResult(
	StockQuantity quantity,
	Order order
) {
}
