package com.kodesalon.kopang.infra.queue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import com.kodesalon.kopang.domain.queue.EventQueueRepository;
import com.kodesalon.kopang.domain.queue.QueueEntry;
import com.kodesalon.kopang.service.exception.DuplicateQueueEntryException;

@Repository
public class EventQueueRepositoryImpl implements EventQueueRepository {

	private static final String QUEUE_KEY = "queue:event:%d";
	private static final String ENTRY_KEY = "queue:entry:%s";
	private static final String ACTIVE_EVENTS = "queue:active_events";
	private static final String ACTIVE_KEY = "queue:active:%d";
	private static final String LOCK_KEY = "queue:lock:%d";
	private static final String MEMBER_KEY = "queue:member:%d:%d";
	private static final long ENTRY_TTL_SEC = 86400L;
	private static final long ACTIVE_TTL_SEC = 300L;
	private static final long LOCK_TTL_SEC = 2L;

	private final StringRedisTemplate redisTemplate;
	private final DefaultRedisScript<List> activateBatchScript;

	public EventQueueRepositoryImpl(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
		this.activateBatchScript = new DefaultRedisScript<>();
		this.activateBatchScript.setLocation(new ClassPathResource("redis/activate_queue_batch.lua"));
		this.activateBatchScript.setResultType(List.class);
	}

	@Override
	public QueueEntry enqueue(Long eventId, Long memberNo, Integer count) {
		String memberKey = String.format(MEMBER_KEY, eventId, memberNo);
		String token = UUID.randomUUID().toString();

		Boolean acquired = redisTemplate.opsForValue()
			.setIfAbsent(memberKey, token, Duration.ofSeconds(ENTRY_TTL_SEC));
		if (!Boolean.TRUE.equals(acquired)) {
			throw DuplicateQueueEntryException.of(eventId, memberNo);
		}

		long requestedAt = System.currentTimeMillis();
		String queueKey = String.format(QUEUE_KEY, eventId);
		String entryKey = String.format(ENTRY_KEY, token);

		redisTemplate.opsForZSet().add(queueKey, token, requestedAt);
		redisTemplate.opsForHash().putAll(entryKey, Map.of(
			"memberNo", String.valueOf(memberNo),
			"count", String.valueOf(count),
			"eventId", String.valueOf(eventId)
		));
		redisTemplate.expire(entryKey, Duration.ofSeconds(ENTRY_TTL_SEC));
		redisTemplate.opsForSet().add(ACTIVE_EVENTS, String.valueOf(eventId));

		return new QueueEntry(token, eventId, memberNo, count, requestedAt);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<QueueEntry> activateNextBatch(Long eventId, int batchSize) {
		List<String> keys = List.of(
			String.format(QUEUE_KEY, eventId),
			String.format(ACTIVE_KEY, eventId),
			ACTIVE_EVENTS
		);
		List<String> popped = (List<String>) redisTemplate.execute(
			activateBatchScript, keys,
			String.valueOf(batchSize), String.valueOf(ACTIVE_TTL_SEC), String.valueOf(eventId)
		);
		if (popped == null || popped.isEmpty()) {
			return List.of();
		}

		// popped = [token, score, token, score, ...] (score 오름차순). 토큰은 이미 활성 Set 에 있다
		List<QueueEntry> entries = new ArrayList<>();
		for (int i = 0; i + 1 < popped.size(); i += 2) {
			String token = popped.get(i);
			long requestedAt = (long) Double.parseDouble(popped.get(i + 1));

			Map<Object, Object> fields = redisTemplate.opsForHash().entries(String.format(ENTRY_KEY, token));
			if (fields.isEmpty()) continue;

			Long entryMemberNo = Long.parseLong((String) fields.get("memberNo"));
			Integer entryCount = Integer.parseInt((String) fields.get("count"));
			Long entryEventId = Long.parseLong((String) fields.get("eventId"));

			entries.add(new QueueEntry(token, entryEventId, entryMemberNo, entryCount, requestedAt));
		}
		return entries;
	}

	@Override
	public long getPosition(Long eventId, String token) {
		String queueKey = String.format(QUEUE_KEY, eventId);
		Long rank = redisTemplate.opsForZSet().rank(queueKey, token);
		return rank == null ? -1L : rank;
	}

	@Override
	public Set<Long> getActiveEventIds() {
		Set<String> members = redisTemplate.opsForSet().members(ACTIVE_EVENTS);
		if (members == null || members.isEmpty()) {
			return Set.of();
		}
		return members.stream()
			.map(Long::parseLong)
			.collect(Collectors.toSet());
	}

	@Override
	public boolean isActive(Long eventId, String token) {
		String activeKey = String.format(ACTIVE_KEY, eventId);
		return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(activeKey, token));
	}

	@Override
	public Optional<Long> findEventIdByToken(String token) {
		String entryKey = String.format(ENTRY_KEY, token);
		Object value = redisTemplate.opsForHash().get(entryKey, "eventId");
		if (value == null) {
			return Optional.empty();
		}
		return Optional.of(Long.parseLong((String) value));
	}

	@Override
	public boolean acquireLock(Long eventId) {
		String lockKey = String.format(LOCK_KEY, eventId);
		Boolean acquired = redisTemplate.opsForValue()
			.setIfAbsent(lockKey, "1", Duration.ofSeconds(LOCK_TTL_SEC));
		return Boolean.TRUE.equals(acquired);
	}
}
