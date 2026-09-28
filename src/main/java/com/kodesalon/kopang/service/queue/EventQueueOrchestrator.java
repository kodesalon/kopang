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

	/**
	 * 토큰은 대기열(WAITING)에서 활성 Set(ACTIVE)으로 한 방향으로만 옮겨진다.
	 * 앞 단계인 대기열을 먼저 보고 활성 Set 을 나중에 봐야, 두 조회 사이에 옮겨진 토큰이 어디에도 없는 것(EXPIRED)으로 보이지 않는다.
	 */
	public QueueStatusResult getStatus(Long eventId, String token) {
		long position = eventQueueService.getPosition(eventId, token);
		if (position >= 0) {
			return new QueueStatusResult(QueueStatus.WAITING, position);
		}
		if (eventQueueService.isTokenActive(eventId, token)) {
			return new QueueStatusResult(QueueStatus.ACTIVE, null);
		}
		return new QueueStatusResult(QueueStatus.EXPIRED, null);
	}
}
