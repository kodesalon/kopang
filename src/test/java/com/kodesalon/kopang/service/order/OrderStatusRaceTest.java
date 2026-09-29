package com.kodesalon.kopang.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.domain.order.OrderStatus;
import com.kodesalon.kopang.domain.order.Orders;
import com.kodesalon.kopang.fixtures.FakePaymentClient;
import com.kodesalon.kopang.scheduler.OrderReconcileScheduler;
import com.kodesalon.kopang.service.payment.PaymentOrchestrator;
import com.kodesalon.kopang.service.purchase.PurchaseOrchestrator;

/**
 * FakePaymentClient 를 @Primary 로 등록해 전용 컨텍스트가 뜬다.
 * 이 컨텍스트의 스케줄러(대기열 워커 등)가 다른 테스트의 Redis 키를 건드리지 않도록 클래스가 끝나면 닫는다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrderStatusRaceTest {

	private static final Long PRODUCT_NO = 9001L;
	private static final Long WAREHOUSE_NO = 9001L;
	private static final BigDecimal PRICE = BigDecimal.valueOf(1000);
	private static final String STOCK_KEY = "stock:product:9001:warehouse:9001";

	@TestConfiguration
	static class FakePaymentClientConfig {

		@Bean
		@Primary
		FakePaymentClient fakePaymentClient() {
			return new FakePaymentClient();
		}
	}

	private @Autowired OrderService orderService;
	private @Autowired PaymentOrchestrator paymentOrchestrator;
	private @Autowired PurchaseOrchestrator purchaseOrchestrator;
	private @Autowired OrderReconcileScheduler orderReconcileScheduler;
	private @Autowired FakePaymentClient fakePaymentClient;
	private @Autowired StringRedisTemplate redisTemplate;
	private @Autowired JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		fakePaymentClient.reset();
	}

	@AfterEach
	void cleanUpRedis() {
		redisTemplate.delete(STOCK_KEY);
	}

	@DisplayName("결제 요청끼리의 경합")
	@Nested
	class ConcurrentPayment {

		@DisplayName("결제 대기 주문에 결제 요청 두 개가 동시에 들어올 때, PG 승인은 한 번만 호출되고 결제는 한 건만 남는다")
		@Test
		void executePayment_ConcurrentRequestsForSameOrder_ApprovesOnlyOnce() throws Exception {
			// given — 첫 요청이 PG 응답을 기다리는 동안 둘째 요청이 들어오도록 승인을 늦춘다
			Order order = orderService.createOrderPending(1L, PRODUCT_NO, WAREHOUSE_NO, 1, PRICE);
			fakePaymentClient.delayApprove(Duration.ofMillis(300));

			int threadCount = 2;
			ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
			CountDownLatch readyLatch = new CountDownLatch(threadCount);
			CountDownLatch startLatch = new CountDownLatch(1);
			List<Future<?>> futures = new ArrayList<>();

			// when
			for (int i = 0; i < threadCount; i++) {
				String paymentKey = "key-" + i;
				futures.add(executorService.submit(() -> {
					readyLatch.countDown();
					startLatch.await();
					return paymentOrchestrator.executePayment(paymentKey, order.getNo(), PRICE, PRODUCT_NO, 1);
				}));
			}
			readyLatch.await();
			startLatch.countDown();

			int succeeded = 0;
			int failed = 0;
			for (Future<?> future : futures) {
				try {
					future.get(10, TimeUnit.SECONDS);
					succeeded++;
				} catch (ExecutionException e) {
					failed++;
				}
			}
			executorService.shutdown();

			// then
			int succeededCount = succeeded;
			int failedCount = failed;
			Integer paymentRows = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM payments WHERE order_no = ?", Integer.class, order.getNo());
			assertAll(
				() -> assertThat(fakePaymentClient.approveCount()).isEqualTo(1),
				() -> assertThat(succeededCount).isEqualTo(1),
				() -> assertThat(failedCount).isEqualTo(1),
				() -> assertThat(statusOf(order)).isEqualTo(OrderStatus.PAID),
				() -> assertThat(paymentRows).isEqualTo(1)
			);
		}
	}

	@DisplayName("만료 주문 일괄 취소")
	@Nested
	class CancelExpiredOrders {

		@DisplayName("만료된 결제 대기 주문을 일괄 취소할 때, 조회 뒤 결제를 시작한 주문이 섞여 있으면, 그 주문은 취소하지 않고 재고도 되돌리지 않는다")
		@Test
		void cancelInBatch_OrderMovedToPaymentInProgress_SkipsItAndRestoresOnlyCancelledStock() {
			// given — 스케줄러가 조회한 시점에는 두 주문 모두 PENDING 이었다
			redisTemplate.opsForValue().set(STOCK_KEY, "0");
			Order expired = orderService.createOrderPending(1L, PRODUCT_NO, WAREHOUSE_NO, 1, PRICE);
			Order paying = orderService.createOrderPending(2L, PRODUCT_NO, WAREHOUSE_NO, 1, PRICE);
			Orders foundByScheduler = new Orders(List.of(expired, paying));
			// 조회와 취소 사이에 사용자가 결제를 시작했다
			orderService.prepareOrderForPayment(paying.getNo(), PRICE);

			// when
			purchaseOrchestrator.cancelInBatch(foundByScheduler);

			// then
			assertAll(
				() -> assertThat(statusOf(expired)).isEqualTo(OrderStatus.CANCELLED),
				() -> assertThat(statusOf(paying)).isEqualTo(OrderStatus.PAYMENT_IN_PROGRESS),
				() -> assertThat(redisTemplate.opsForValue().get(STOCK_KEY)).isEqualTo("1")
			);
		}
	}

	@DisplayName("결제 중 주문 대사")
	@Nested
	class ReconcileStuckPaymentOrders {

		@DisplayName("PG 조회가 계속 실패하는 결제 중 주문이 있을 때, 대사 스케줄러를 실행하면, 같은 주문을 반복 조회하지 않고 한 번 돈 뒤 끝난다")
		@Test
		void reconcileStuckPaymentOrders_PgLookupKeepsFailing_ReturnsAfterOnePass() {
			// given — 결제 중 상태로 20분이 지난 주문. FakePaymentClient 의 결제 조회는 항상 실패한다
			Order order = orderService.createOrderPending(1L, PRODUCT_NO, WAREHOUSE_NO, 1, PRICE);
			orderService.prepareOrderForPayment(order.getNo(), PRICE);
			jdbcTemplate.update("UPDATE orders SET ordered_at = ? WHERE no = ?",
				LocalDateTime.now().minusMinutes(20), order.getNo());

			// when
			assertTimeoutPreemptively(Duration.ofSeconds(5), () -> orderReconcileScheduler.reconcileStuckPaymentOrders());

			// then — 백그라운드 스케줄러가 같은 시각에 한 번 더 돌 수 있어 최대 2번까지 허용한다
			assertAll(
				() -> assertThat(fakePaymentClient.retrieveCount()).isBetween(1, 2),
				() -> assertThat(statusOf(order)).isEqualTo(OrderStatus.PAYMENT_IN_PROGRESS)
			);
		}
	}

	private OrderStatus statusOf(Order order) {
		String status = jdbcTemplate.queryForObject("SELECT status FROM orders WHERE no = ?", String.class, order.getNo());
		return OrderStatus.valueOf(status);
	}
}
