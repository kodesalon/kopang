package com.kodesalon.kopang.scheduler;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kodesalon.kopang.service.queue.EventQueueService;

/**
 * 테스트에서는 kopang.queue.worker.enabled=false 로 끈다.
 * 켜 두면 테스트가 넣은 대기열 항목을 이 워커가 먼저 가져간다.
 */
@Component
@ConditionalOnProperty(prefix = "kopang.queue.worker", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EventQueueWorker {

	private static final Logger log = LoggerFactory.getLogger(EventQueueWorker.class);
	private static final int BATCH_SIZE = 400;

	private final EventQueueService eventQueueService;

	public EventQueueWorker(EventQueueService eventQueueService) {
		this.eventQueueService = eventQueueService;
	}

	@Scheduled(fixedDelay = 500)
	void processQueue() {
		Set<Long> activeEventIds = eventQueueService.getActiveEventIds();
		for (Long eventId : activeEventIds) {
			if (!eventQueueService.acquireLock(eventId)) {
				continue;
			}
			try {
				eventQueueService.activateNextBatch(eventId, BATCH_SIZE);
			} catch (Exception e) {
				log.warn("대기열 활성화 실패: eventId={}, reason={}", eventId, e.getMessage());
			}
		}
	}
}
