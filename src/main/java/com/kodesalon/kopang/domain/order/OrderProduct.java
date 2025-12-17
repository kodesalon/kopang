package com.kodesalon.kopang.domain.order;

import java.math.BigDecimal;

public class OrderProduct {

	private final Long no;
	private final Long productNo;
	private final Integer count;
	private final Money orderPrice;

	public OrderProduct(Long no, Long productNo, Integer count, Money orderPrice) {
		this.no = no;
		this.productNo = productNo;
		this.count = count;
		this.orderPrice = orderPrice;
	}

	public static OrderProduct create(Long productNo, Integer count, BigDecimal productPrice) {
		Money orderPrice = new Money(productPrice.multiply(BigDecimal.valueOf(count)));
		return new OrderProduct(null, productNo, count, orderPrice);
	}

	public Long getProductNo() {
		return productNo;
	}

	public Integer getCount() {
		return count;
	}

	public Money getOrderPrice() {
		return orderPrice;
	}
}
