package com.kodesalon.kopang.external;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.kodesalon.kopang.domain.payment.PaymentClient;
import com.kodesalon.kopang.domain.payment.PaymentResult;

@Component
public class MockPaymentClient implements PaymentClient {

	/**
	 * orderNo 를 통한 멱등키 생성 및 멱등키 관리 필요
	 */

	@Override
	public PaymentResult approve(String paymentKey, Long orderNo, BigDecimal amount) {
		return null;
	}

	@Override
	public PaymentResult retrieveByOrder(Long orderNo) {
		return null;
	}
}
