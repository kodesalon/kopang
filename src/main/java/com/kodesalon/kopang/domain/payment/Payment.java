package com.kodesalon.kopang.domain.payment;

import java.time.LocalDateTime;

import com.kodesalon.kopang.domain.order.Money;

public class Payment {

	private final Long no;
	private final Long orderNo;
	private final String paymentKey;
	private final Money amount;
	private final LocalDateTime approvedAt;

	public Payment(Long no, Long orderNo, String paymentKey, Money amount, LocalDateTime approvedAt) {
		this.no = no;
		this.orderNo = orderNo;
		this.paymentKey = paymentKey;
		this.amount = amount;
		this.approvedAt = approvedAt;
	}
}
