package com.kodesalon.kopang.service.queue;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.kodesalon.kopang.domain.queue.EventQueueRepository;
import com.kodesalon.kopang.domain.queue.QueueEntry;

@Service
public class EventQueueService {

	private final EventQueueRepository eventQueueRepository;

	public EventQueueService(EventQueueRepository eventQueueRepository) {
		this.eventQueueRepository = eventQueueRepository;
	}

	public QueueEntry enqueue(Long eventId, Long memberNo, Integer count) {
		return eventQueueRepository.enqueue(eventId, memberNo, count);
	}

	public List<QueueEntry> dequeueForProcessing(Long eventId, int batchSize) {
		return eventQueueRepository.dequeueForProcessing(eventId, batchSize);
	}

	public long getPosition(Long eventId, String token) {
		return eventQueueRepository.getPosition(eventId, token);
	}

	public Set<Long> getActiveEventIds() {
		return eventQueueRepository.getActiveEventIds();
	}

	public void activateTokens(Long eventId, List<String> tokens) {
		eventQueueRepository.activateTokens(eventId, tokens);
	}

	public boolean isTokenActive(Long eventId, String token) {
		return eventQueueRepository.isActive(eventId, token);
	}

	public Optional<Long> findEventIdByToken(String token) {
		return eventQueueRepository.findEventIdByToken(token);
	}

	public boolean acquireLock(Long eventId) {
		return eventQueueRepository.acquireLock(eventId);
	}
}
