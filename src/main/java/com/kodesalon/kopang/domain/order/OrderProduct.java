package com.kodesalon.kopang.domain.order;

import java.math.BigDecimal;

public class OrderProduct {

	private final Long no;
	private final Long productNo;
	private final Long warehouseNo;
	private final Integer count;
	private final Money orderPrice;

	private OrderProduct(Long no, Long productNo, Long warehouseNo, Integer count, Money orderPrice) {
		this.no = no;
		this.productNo = productNo;
		this.warehouseNo = warehouseNo;
		this.count = count;
		this.orderPrice = orderPrice;
	}

	public static OrderProduct create(Long productNo, Long warehouseNo, Integer count, BigDecimal productPrice) {
		Money orderPrice = new Money(productPrice.multiply(BigDecimal.valueOf(count)));
		return new OrderProduct(null, productNo, warehouseNo, count, orderPrice);
	}

	public static OrderProduct of(Long no, Long productNo, Long warehouseNo, Integer count, BigDecimal orderPrice) {
		return new OrderProduct(no, productNo, warehouseNo, count, new Money(orderPrice));
	}

	public Long getProductNo() {
		return productNo;
	}

	public Long getWarehouseNo() {
		return warehouseNo;
	}

	public Integer getCount() {
		return count;
	}

	public Money getOrderPrice() {
		return orderPrice;
	}
}
