package com.kodesalon.kopang.domain.queue;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EventQueueRepository {

	// 대기열 진입 (ZADD + HSET + SADD)
	QueueEntry enqueue(Long eventId, Long memberNo, Integer count);

	// 앞쪽 batchSize 개를 WAITING → ACTIVE 로 옮긴다 (ZPOPMIN + SADD + EXPIRE 를 Lua 로 한 번에). 옮긴 항목을 FIFO 순서로 반환
	List<QueueEntry> activateNextBatch(Long eventId, int batchSize);

	// 대기 순위 조회 (ZRANK, 0-based, 없으면 -1)
	long getPosition(Long eventId, String token);

	// 활성 이벤트 목록 (SMEMBERS queue:active_events)
	Set<Long> getActiveEventIds();

	// ACTIVE 여부 확인 (SISMEMBER queue:active:{eventId})
	boolean isActive(Long eventId, String token);

	// token → eventId 역조회 (HGET queue:entry:{token} eventId)
	Optional<Long> findEventIdByToken(String token);

	// eventId 단위 락 획득 (SET NX EX)
	boolean acquireLock(Long eventId);
}
