package com.kodesalon.kopang.service.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.kodesalon.kopang.domain.queue.QueueEntry;
import com.kodesalon.kopang.domain.queue.QueueStatus;

@SpringBootTest
@ActiveProfiles("test")
class EventQueueWorkerConcurrencyTest {

	private static final Long EVENT_ID = 1L;
	private static final String QUEUE_EVENT_KEY = "queue:event:1";
	private static final String QUEUE_ACTIVE_EVENTS_KEY = "queue:active_events";
	private static final String QUEUE_ACTIVE_KEY = "queue:active:1";
	private static final String QUEUE_LOCK_KEY = "queue:lock:1";

	@Autowired
	EventQueueService eventQueueService;

	@Autowired
	EventQueueOrchestrator eventQueueOrchestrator;

	@Autowired
	StringRedisTemplate redisTemplate;

	// 중복 진입 방지 키(queue:member:*)는 24시간 남는다. 지우지 않으면 다음 테스트의 같은 회원 진입이 거절된다
	@BeforeEach
	@AfterEach
	void cleanUpRedis() {
		redisTemplate.delete(List.of(QUEUE_EVENT_KEY, QUEUE_ACTIVE_EVENTS_KEY, QUEUE_ACTIVE_KEY, QUEUE_LOCK_KEY));
		for (String pattern : List.of("queue:entry:*", "queue:member:1:*")) {
			Set<String> keys = redisTemplate.keys(pattern);
			if (keys != null && !keys.isEmpty()) {
				redisTemplate.delete(keys);
			}
		}
	}

	@Nested
	@DisplayName("activateNextBatch — 대기열 앞쪽 활성화")
	class ActivateNextBatch {

		@Test
		@DisplayName("50명이 동시에 진입한 뒤 10명씩 활성화하면, 먼저 진입한 항목(requestedAt 오름차순)부터 활성화된다")
		void activateNextBatch_FiftyConcurrentEntries_ReturnsInFifoOrder() throws InterruptedException {
			// given
			int threadCount = 50;
			enqueueConcurrently(threadCount);

			// when — 10명씩 5번
			List<QueueEntry> allActivated = new ArrayList<>();
			for (int batch = 0; batch < 5; batch++) {
				allActivated.addAll(eventQueueService.activateNextBatch(EVENT_ID, 10));
			}

			// then
			assertThat(allActivated).hasSize(threadCount);
			for (int i = 0; i < allActivated.size() - 1; i++) {
				assertThat(allActivated.get(i).requestedAt())
					.as("index %d requestedAt should be <= index %d requestedAt (FIFO order)", i, i + 1)
					.isLessThanOrEqualTo(allActivated.get(i + 1).requestedAt());
			}
		}

		@Test
		@DisplayName("50명이 동시에 진입한 뒤 10명씩 활성화하면, 진입한 토큰이 모두 ACTIVE Set 에 등록된다")
		void activateNextBatch_FiftyConcurrentEntries_ActivatesAllTokens() throws InterruptedException {
			// given
			List<String> enqueuedTokens = enqueueConcurrently(50);

			// when
			for (int batch = 0; batch < 5; batch++) {
				eventQueueService.activateNextBatch(EVENT_ID, 10);
			}

			// then
			assertThat(enqueuedTokens).hasSize(50);
			for (String token : enqueuedTokens) {
				assertThat(eventQueueService.isTokenActive(EVENT_ID, token))
					.as("token %s should be ACTIVE", token)
					.isTrue();
			}
		}

		@Test
		@DisplayName("400명을 한 번에 활성화하는 동안 상태를 계속 조회할 때, 대기열에서 꺼낸 토큰이 EXPIRED 로 보이지 않는다")
		void activateNextBatch_StatusPolledDuringActivation_NeverExpired() throws Exception {
			// given
			int entryCount = 400;
			List<String> tokens = new ArrayList<>();
			for (int i = 0; i < entryCount; i++) {
				tokens.add(eventQueueService.enqueue(EVENT_ID, i + 1L, 1).token());
			}

			AtomicBoolean activating = new AtomicBoolean(true);
			AtomicInteger polls = new AtomicInteger();
			AtomicInteger expiredSeen = new AtomicInteger();
			CountDownLatch pollerStarted = new CountDownLatch(1);
			ExecutorService poller = Executors.newSingleThreadExecutor();
			Future<?> pollTask = poller.submit(() -> {
				pollerStarted.countDown();
				int i = 0;
				while (activating.get()) {
					String token = tokens.get(i++ % entryCount);
					if (eventQueueOrchestrator.getStatus(EVENT_ID, token).status() == QueueStatus.EXPIRED) {
						expiredSeen.incrementAndGet();
					}
					polls.incrementAndGet();
				}
			});
			pollerStarted.await();

			// when
			List<QueueEntry> activated = eventQueueService.activateNextBatch(EVENT_ID, entryCount);
			activating.set(false);
			pollTask.get(5, TimeUnit.SECONDS);
			poller.shutdown();

			// then
			assertAll(
				() -> assertThat(activated).hasSize(entryCount),
				() -> assertThat(polls.get()).isPositive(),
				() -> assertThat(expiredSeen.get()).isZero()
			);
		}

		@Test
		@DisplayName("대기열에 항목이 남아 있으면 활성 이벤트 목록에 남고, 마지막 항목까지 활성화하면 목록에서 빠진다")
		void activateNextBatch_QueueDrained_RemovesEventFromActiveEvents() {
			// given
			for (long memberNo = 1; memberNo <= 3; memberNo++) {
				eventQueueService.enqueue(EVENT_ID, memberNo, 1);
			}

			// when
			eventQueueService.activateNextBatch(EVENT_ID, 2);
			Set<Long> afterFirstBatch = eventQueueService.getActiveEventIds();
			eventQueueService.activateNextBatch(EVENT_ID, 2);
			Set<Long> afterLastBatch = eventQueueService.getActiveEventIds();

			// then
			assertAll(
				() -> assertThat(afterFirstBatch).contains(EVENT_ID),
				() -> assertThat(afterLastBatch).doesNotContain(EVENT_ID)
			);
		}
	}

	private List<String> enqueueConcurrently(int threadCount) throws InterruptedException {
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(threadCount);
		List<String> tokens = new ArrayList<>();

		for (int i = 0; i < threadCount; i++) {
			final long memberNo = i + 1L;
			executorService.submit(() -> {
				try {
					startLatch.await();
					QueueEntry entry = eventQueueService.enqueue(EVENT_ID, memberNo, 1);
					synchronized (tokens) {
						tokens.add(entry.token());
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					doneLatch.countDown();
				}
			});
		}

		startLatch.countDown();
		doneLatch.await();
		executorService.shutdown();
		return tokens;
	}
}
