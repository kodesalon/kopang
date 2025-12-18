package com.kodesalon.kopang.service.purchase;

import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.domain.stock.Stock;

public record ReservationOrderResult(
	Stock stock,
	Order order
) {
}
