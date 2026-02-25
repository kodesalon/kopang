package com.kodesalon.kopang.external;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import com.kodesalon.kopang.domain.order.Money;
import com.kodesalon.kopang.domain.payment.PaymentClient;
import com.kodesalon.kopang.domain.payment.PaymentResult;

/**
 * 외부 결제 PG 연동의 Fake 구현체입니다.
 * 프로덕션 환경에서는 실제 PG사 연동으로 교체되어야 합니다.
 * 테스트 환경에서 {@link #setNextResult(PaymentResult)} 를 통해 다음 호출에 반환할 결과를 주입할 수 있습니다.
 * 설정된 결과가 없으면 기본적으로 DONE 상태의 성공 결과를 반환합니다.
 */
@Component
public class MockPaymentClient implements PaymentClient {

	private final AtomicReference<PaymentResult> nextResult = new AtomicReference<>();

	/**
	 * 다음 approve 호출에 반환할 결과를 설정합니다. (테스트 전용)
	 */
	public void setNextResult(PaymentResult result) {
		nextResult.set(result);
	}

	/**
	 * 설정된 결과를 초기화합니다. (테스트 전용)
	 */
	public void reset() {
		nextResult.set(null);
	}

	@Override
	public PaymentResult approve(String paymentKey, Long orderNo, BigDecimal amount) {
		PaymentResult configured = nextResult.getAndSet(null);
		if (configured != null) {
			return configured;
		}
		return new PaymentResult(
			paymentKey,
			orderNo,
			new Money(amount),
			LocalDateTime.now(),
			PaymentResult.Status.DONE,
			null
		);
	}

	@Override
	public PaymentResult retrieveByOrder(Long orderNo) {
		return null;
	}
}
