package com.kodesalon.kopang.domain.queue;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EventQueueRepository {

	// 대기열 진입 (ZADD + HSET + SADD)
	QueueEntry enqueue(Long eventId, Long memberNo, Integer count);

	// FIFO dequeue (ZPOPMIN)
	List<QueueEntry> dequeueForProcessing(Long eventId, int batchSize);

	// 대기 순위 조회 (ZRANK, 0-based, 없으면 -1)
	long getPosition(Long eventId, String token);

	// 활성 이벤트 목록 (SMEMBERS queue:active_events)
	Set<Long> getActiveEventIds();

	// WAITING → ACTIVE 이동 (SADD + EXPIRE)
	void activateTokens(Long eventId, List<String> tokens);

	// ACTIVE 여부 확인 (SISMEMBER queue:active:{eventId})
	boolean isActive(Long eventId, String token);

	// token → eventId 역조회 (HGET queue:entry:{token} eventId)
	Optional<Long> findEventIdByToken(String token);

	// eventId 단위 락 획득 (SET NX EX)
	boolean acquireLock(Long eventId);
}
