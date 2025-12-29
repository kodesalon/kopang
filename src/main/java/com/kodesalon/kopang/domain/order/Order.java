package com.kodesalon.kopang.domain.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Order {

	private final Long no;
	private final Long memberNo;
	private final OrderStatus status;
	private final Money totalPrice;
	private final List<OrderProduct> products;

	public Order preparePayment(Money amount) {
		if (!amount.equals(totalPrice)) {
			throw new IllegalArgumentException("주문 금액과 결제 금액이 일치하지 않습니다");
		}
		if (status.isPaid()) {
			throw new IllegalStateException("[중복 결제 불가능] 이미 결제된 주문입니다");
		} else if (status.isCanceled()) {
			throw new IllegalStateException("[취소된 주문] 이미 취소된 주문입니다");
		}
		return new Order(no, memberNo, OrderStatus.PAYMENT_IN_PROGRESS, totalPrice, products);
	}

	public Order rollbackToPending() {
		if (status.isPaid()) {
			throw new IllegalStateException("[중복 결제 불가능] 이미 결제된 주문입니다");
		} else if (status.isCanceled()) {
			throw new IllegalStateException("[취소된 주문] 이미 취소된 주문입니다");
		}
		return new Order(no, memberNo, OrderStatus.PENDING, totalPrice, products);
	}

	public Order pay() {
		if (status.isPaid()) {
			throw new IllegalStateException("[중복 결제 불가능] 이미 결제된 주문입니다");
		} else if (status.isCanceled()) {
			throw new IllegalStateException("[취소된 주문] 이미 취소된 주문입니다");
		} else if (status.isPending()) {
			throw new IllegalStateException("[결제 대기 주문] 결제 대기중인 주문입니다");
		}
		return new Order(no, memberNo, OrderStatus.PAID, totalPrice, products);
	}

	public Order cancel() {
		if (status.isPaid()) {
			throw new IllegalStateException("[취소된 주문] 이미 결제된 주문입니다");
		} else if (status.isCanceled()) {
			throw new IllegalStateException("[중복 취소 불가능] 이미 취소된 주문입니다");
		}
		return new Order(no, memberNo, OrderStatus.CANCELLED, totalPrice, products);
	}

	public static Order createPending(Long memberNo, Long productNo, Integer count, BigDecimal productPrice) {
		List<OrderProduct> orderProducts = new ArrayList<>();
		orderProducts.add(OrderProduct.create(productNo, count, productPrice));
		return new Order(null, memberNo, OrderStatus.PENDING, calculateTotalMoney(orderProducts), orderProducts);
	}

	public static Order of(Long no, Long memberNo, OrderStatus status, List<OrderProduct> products) {
		return new Order(no, memberNo, status, calculateTotalMoney(products), products);
	}

	private static Money calculateTotalMoney(List<OrderProduct> products) {
		return products.stream()
			.map(OrderProduct::getOrderPrice)
			.reduce(Money.ZERO, Money::plus);
	}

	private Order(Long no, Long memberNo, OrderStatus status, Money totalPrice, List<OrderProduct> products) {
		this.no = no;
		this.memberNo = memberNo;
		this.status = status;
		this.totalPrice = totalPrice;
		this.products = products;
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
