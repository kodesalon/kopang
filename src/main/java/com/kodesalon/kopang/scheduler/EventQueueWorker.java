package com.kodesalon.kopang.scheduler;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kodesalon.kopang.domain.queue.QueueEntry;
import com.kodesalon.kopang.service.queue.EventQueueService;

@Component
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
				List<QueueEntry> entries = eventQueueService.dequeueForProcessing(eventId, BATCH_SIZE);
				if (entries.isEmpty()) {
					continue;
				}
				List<String> tokens = entries.stream().map(QueueEntry::token).toList();
				eventQueueService.activateTokens(eventId, tokens);
			} catch (Exception e) {
				log.warn("대기열 활성화 실패: eventId={}, reason={}", eventId, e.getMessage());
			}
		}
	}
}
