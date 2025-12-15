package com.kodesalon.kopang.domain.order;

import java.util.List;

public class OrderFixture {

	static final OrderProduct FIRST_ORDER_PRODUCT = new OrderProduct(1L, 1L, 1, new Money(1000L));
	static final Order PENDING_ORDER = Order.createPending(1L, 1L, List.of(FIRST_ORDER_PRODUCT));
	static final Order PAID_ORDER = PENDING_ORDER.pay();
	static final Order CANCELLED_ORDER = PENDING_ORDER.cancel();
}
