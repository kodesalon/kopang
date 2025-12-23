package com.kodesalon.kopang.storage.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

	@Column(nullable = false)
	private LocalDateTime approvedAt;

	protected PaymentJpaEntity() {
	}

	public PaymentJpaEntity(Long orderNo, String paymentKey, BigDecimal amount) {
		this.orderNo = orderNo;
		this.paymentKey = paymentKey;
		this.amount = amount;
		this.approvedAt = LocalDateTime.now();
	}
}
