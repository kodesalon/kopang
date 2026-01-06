package com.kodesalon.kopang.storage.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kodesalon.kopang.domain.order.Money;
import com.kodesalon.kopang.domain.payment.Payment;
import com.kodesalon.kopang.domain.payment.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payments")
public class PaymentJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long no;

	@Column(nullable = false)
	private Long orderNo;

	@Column(nullable = false)
	private String paymentKey;

	@Column(nullable = false)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentStatus status;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	private String failureReason;

	protected PaymentJpaEntity() {
	}

	public static PaymentJpaEntity from(Payment payment) {
		return new PaymentJpaEntity(
			payment.getOrderNo(),
			payment.getPaymentKey(),
			payment.getAmount().getAmount(),
			payment.getStatus(),
			payment.getCreatedAt(),
			payment.getFailureReason()
		);
	}

	public Payment toDomain() {
		return new Payment(no, orderNo, paymentKey, new Money(amount), status, createdAt, failureReason);
	}

	private PaymentJpaEntity(Long orderNo, String paymentKey, BigDecimal amount, PaymentStatus status,
		LocalDateTime createdAt, String failureReason) {
		this.orderNo = orderNo;
		this.paymentKey = paymentKey;
		this.amount = amount;
		this.status = status;
		this.createdAt = createdAt;
		this.failureReason = failureReason;
	}
}
