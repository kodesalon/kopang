package com.kodesalon.kopang.service.queue;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.kodesalon.kopang.domain.queue.QueueEntry;

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
	StringRedisTemplate redisTemplate;

	@AfterEach
	void cleanUpRedis() {
		redisTemplate.delete(QUEUE_EVENT_KEY);
		redisTemplate.delete(QUEUE_ACTIVE_EVENTS_KEY);
		redisTemplate.delete(QUEUE_ACTIVE_KEY);
		redisTemplate.delete(QUEUE_LOCK_KEY);

		Set<String> entryKeys = redisTemplate.keys("queue:entry:*");
		if (entryKeys != null && !entryKeys.isEmpty()) {
			redisTemplate.delete(entryKeys);
		}
	}

	@Nested
	@DisplayName("dequeueForProcessing — FIFO 순서 보장")
	class DequeueForProcessing {

		@Test
		@DisplayName("50개 동시 진입 후 Worker가 dequeue하면, 먼저 진입한 항목(requestedAt 오름차순)이 먼저 활성화된다")
		void dequeueForProcessing_FiftyConcurrentEntries_ReturnsInFifoOrder() throws InterruptedException {
			// given
			int threadCount = 50;
			int batchSize = 10;
			ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
			CountDownLatch startLatch = new CountDownLatch(1);
			CountDownLatch doneLatch = new CountDownLatch(threadCount);

			for (int i = 0; i < threadCount; i++) {
				final long memberNo = i + 1L;
				executorService.submit(() -> {
					try {
						startLatch.await();
						eventQueueService.enqueue(EVENT_ID, memberNo, 1);
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

			// when — dequeue in 5 batches of 10
			List<QueueEntry> allDequeued = new ArrayList<>();
			for (int batch = 0; batch < 5; batch++) {
				// release any lock from previous iteration
				redisTemplate.delete(QUEUE_LOCK_KEY);
				List<QueueEntry> entries = eventQueueService.dequeueForProcessing(EVENT_ID, batchSize);
				allDequeued.addAll(entries);
			}

			// then — all 50 entries dequeued, sorted by requestedAt ascending (FIFO)
			assertThat(allDequeued).hasSize(threadCount);

			for (int i = 0; i < allDequeued.size() - 1; i++) {
				long current = allDequeued.get(i).requestedAt();
				long next = allDequeued.get(i + 1).requestedAt();
				assertThat(current)
					.as("index %d requestedAt should be <= index %d requestedAt (FIFO order)", i, i + 1)
					.isLessThanOrEqualTo(next);
			}
		}

		@Test
		@DisplayName("50개 동시 진입 후 Worker가 배치 dequeue하면, ACTIVE Set에 token이 정확히 등록된다")
		void dequeueForProcessing_FiftyConcurrentEntries_ActivatesTokensInActiveSet() throws InterruptedException {
			// given
			int threadCount = 50;
			ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
			CountDownLatch startLatch = new CountDownLatch(1);
			CountDownLatch doneLatch = new CountDownLatch(threadCount);
			List<String> enqueuedTokens = new ArrayList<>();

			for (int i = 0; i < threadCount; i++) {
				final long memberNo = i + 1L;
				executorService.submit(() -> {
					try {
						startLatch.await();
						QueueEntry entry = eventQueueService.enqueue(EVENT_ID, memberNo, 1);
						synchronized (enqueuedTokens) {
							enqueuedTokens.add(entry.token());
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

			// when — simulate Worker: dequeue batches and activate
			List<String> activatedTokens = new ArrayList<>();
			for (int batch = 0; batch < 5; batch++) {
				redisTemplate.delete(QUEUE_LOCK_KEY);
				List<QueueEntry> entries = eventQueueService.dequeueForProcessing(EVENT_ID, 10);
				if (!entries.isEmpty()) {
					List<String> tokens = entries.stream().map(QueueEntry::token).toList();
					eventQueueService.activateTokens(EVENT_ID, tokens);
					activatedTokens.addAll(tokens);
				}
			}

			// then — all dequeued tokens are in the ACTIVE Set
			assertThat(activatedTokens).hasSize(threadCount);
			for (String token : activatedTokens) {
				assertThat(eventQueueService.isTokenActive(EVENT_ID, token))
					.as("token %s should be ACTIVE", token)
					.isTrue();
			}
		}
	}
}
