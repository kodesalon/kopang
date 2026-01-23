package com.kodesalon.kopang.domain.payment;

import java.time.LocalDateTime;

import com.kodesalon.kopang.domain.order.Money;

public class Payment {

	private final Long no;
	private final Long orderNo;
	private final String paymentKey;
	private final Money amount;
	private final PaymentStatus status;
	private final LocalDateTime createdAt;
	private final String failureReason;

	public static Payment createSuccess(Long orderNo, PaymentResult result) {
		return new Payment(
			null,
			orderNo,
			result.paymentKey(),
			result.amount(),
			PaymentStatus.DONE,
			result.approvedAt(),
			null
		);
	}

	public static Payment createFailure(Long orderNo, PaymentResult result) {
		return new Payment(
			null,
			orderNo,
			result.paymentKey(),
			result.amount(),
			PaymentStatus.ABORTED,
			result.approvedAt(),
			result.failureMessage()
		);
	}

	public Payment(
		Long no, Long orderNo, String paymentKey, Money amount,
		PaymentStatus status, LocalDateTime createdAt, String failureReason
	) {
		this.no = no;
		this.orderNo = orderNo;
		this.paymentKey = paymentKey;
		this.amount = amount;
		this.status = status;
		this.createdAt = createdAt;
		this.failureReason = failureReason;
	}

	public Long getNo() {
		return no;
	}

	public Long getOrderNo() {
		return orderNo;
	}

	public String getPaymentKey() {
		return paymentKey;
	}

	public Money getAmount() {
		return amount;
	}

	public PaymentStatus getStatus() {
		return status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public String getFailureReason() {
		return failureReason;
	}
}
