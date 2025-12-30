package com.kodesalon.kopang.domain.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {

	private final Long no;
	private final Long memberNo;
	private final OrderStatus status;
	private final Money totalPrice;
	private final List<OrderProduct> products;
	private final LocalDateTime orderedAt;

	public Order preparePayment(Money amount) {
		validateEditable();
		validateAmount(amount);
		return new Order(no, memberNo, OrderStatus.PAYMENT_IN_PROGRESS, totalPrice, products, orderedAt);
	}

	public Order rollbackToPending() {
		validateEditable();
		return new Order(no, memberNo, OrderStatus.PENDING, totalPrice, products, orderedAt);
	}

	public Order pay() {
		if (!status.isPaymentInProgress()) {
			throw new IllegalStateException("결제 진행 중인 주문만 승인할 수 있습니다.");
		}
		return new Order(no, memberNo, OrderStatus.PAID, totalPrice, products, orderedAt);
	}

	public Order cancel() {
		if (status.isPaid()) {
			throw new IllegalStateException("이미 결제된 주문은 취소할 수 없습니다.");
		}
		return new Order(no, memberNo, OrderStatus.CANCELLED, totalPrice, products, orderedAt);
	}

	private void validateEditable() {
		if (status.isPaid() || status.isCanceled()) {
			throw new IllegalStateException("이미 처리가 완료된 주문입니다.");
		}
	}

	private void validateAmount(Money amount) {
		if (!totalPrice.equals(amount)) {
			throw new IllegalArgumentException("주문 금액과 결제 금액이 일치하지 않습니다");
		}
	}

	public static Order createPending(Long memberNo, Long productNo, Integer count, BigDecimal productPrice) {
		List<OrderProduct> orderProducts = new ArrayList<>();
		orderProducts.add(OrderProduct.create(productNo, count, productPrice));
		return new Order(null, memberNo, OrderStatus.PENDING, calculateTotalMoney(orderProducts), orderProducts, null);
	}

	public static Order of(Long no, Long memberNo, OrderStatus status, List<OrderProduct> products, LocalDateTime orderedAt) {
		return new Order(no, memberNo, status, calculateTotalMoney(products), products, orderedAt);
	}

	private static Money calculateTotalMoney(List<OrderProduct> products) {
		return products.stream()
			.map(OrderProduct::getOrderPrice)
			.reduce(Money.ZERO, Money::plus);
	}

	private Order(
		Long no, Long memberNo, OrderStatus status, Money totalPrice,
		List<OrderProduct> products, LocalDateTime orderedAt
	) {
		this.no = no;
		this.memberNo = memberNo;
		this.status = status;
		this.totalPrice = totalPrice;
		this.products = products;
		this.orderedAt = orderedAt;
	}

	public Long getNo() {
		return no;
	}

	public Long getMemberNo() {
		return memberNo;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public Money getTotalPrice() {
		return totalPrice;
	}

	public List<OrderProduct> getProducts() {
		return products;
	}
}
