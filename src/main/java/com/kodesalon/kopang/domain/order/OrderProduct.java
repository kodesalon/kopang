package com.kodesalon.kopang.domain.order;

public class OrderProduct {

	private final Long no;
	private final Long productId;
	private final Integer count;
	private final Money orderPrice;

	public OrderProduct(Long no, Long productId, Integer count, Money orderPrice) {
		this.no = no;
		this.productId = productId;
		this.count = count;
		this.orderPrice = orderPrice;
	}

	public Money getOrderPrice() {
		return orderPrice;
	}
}
