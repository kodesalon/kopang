package com.kodesalon.kopang.domain.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderFixture {

	public static final OrderProduct FIRST_ORDER_PRODUCT = OrderProduct.of(1L, 1L, 1, BigDecimal.valueOf(1000));
	public static final Order PENDING_ORDER = Order.of(1L, 1L, OrderStatus.PENDING, List.of(FIRST_ORDER_PRODUCT), LocalDateTime.now());
	public static final Order PAID_ORDER = PENDING_ORDER.pay();
	public static final Order CANCELLED_ORDER = PENDING_ORDER.cancel();
}
