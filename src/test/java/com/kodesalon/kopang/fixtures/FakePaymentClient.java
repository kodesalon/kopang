package com.kodesalon.kopang.fixtures;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import com.kodesalon.kopang.domain.order.Money;
import com.kodesalon.kopang.domain.payment.PaymentClient;
import com.kodesalon.kopang.domain.payment.PaymentResult;

/**
 * 테스트용 PG. 승인은 항상 성공(DONE)하고, 결제 조회는 항상 실패한다.
 * 승인·조회 호출 횟수를 센다.
 */
public class FakePaymentClient implements PaymentClient {

	private final AtomicInteger approveCount = new AtomicInteger();
	private final AtomicInteger retrieveCount = new AtomicInteger();
	private volatile Duration approveDelay = Duration.ZERO;

	@Override
	public PaymentResult approve(String paymentKey, Long orderNo, BigDecimal amount) {
		approveCount.incrementAndGet();
		sleep(approveDelay);
		return new PaymentResult(paymentKey, orderNo, new Money(amount), LocalDateTime.now(), PaymentResult.Status.DONE, null);
	}

	@Override
	public PaymentResult retrieveByOrder(Long orderNo) {
		retrieveCount.incrementAndGet();
		throw new IllegalStateException("PG 결제 조회 실패 (테스트용 Fake)");
	}

	public void delayApprove(Duration delay) {
		this.approveDelay = delay;
	}

	public int approveCount() {
		return approveCount.get();
	}

	public int retrieveCount() {
		return retrieveCount.get();
	}

	public void reset() {
		approveCount.set(0);
		retrieveCount.set(0);
		approveDelay = Duration.ZERO;
	}

	private void sleep(Duration duration) {
		try {
			Thread.sleep(duration.toMillis());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
