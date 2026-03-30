package com.kodesalon.kopang.service.queue;

import org.springframework.stereotype.Component;

import com.kodesalon.kopang.domain.queue.QueueEntry;
import com.kodesalon.kopang.domain.queue.QueueStatus;

@Component
public class EventQueueOrchestrator {

	private final EventQueueService eventQueueService;

	public EventQueueOrchestrator(EventQueueService eventQueueService) {
		this.eventQueueService = eventQueueService;
	}

	public EnterQueueResult enqueue(Long eventId, Long memberNo, Integer count) {
		QueueEntry entry = eventQueueService.enqueue(eventId, memberNo, count);
		long position = eventQueueService.getPosition(eventId, entry.token());
		long estimatedWaitMs = position * 500L;
		return new EnterQueueResult(entry.token(), position, estimatedWaitMs);
	}

	public QueueStatusResult getStatus(Long eventId, String token) {
		if (eventQueueService.isTokenActive(eventId, token)) {
			return new QueueStatusResult(QueueStatus.ACTIVE, null);
		}
		long position = eventQueueService.getPosition(eventId, token);
		if (position >= 0) {
			return new QueueStatusResult(QueueStatus.WAITING, position);
		}
		return new QueueStatusResult(QueueStatus.EXPIRED, null);
	}
}
