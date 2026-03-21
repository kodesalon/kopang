package com.kodesalon.kopang.infra.queue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import com.kodesalon.kopang.domain.queue.EventQueueRepository;
import com.kodesalon.kopang.domain.queue.QueueEntry;

@Repository
public class EventQueueRepositoryImpl implements EventQueueRepository {

	private static final String QUEUE_KEY = "queue:event:%d";
	private static final String ENTRY_KEY = "queue:entry:%s";
	private static final String ACTIVE_EVENTS = "queue:active_events";
	private static final String ACTIVE_KEY = "queue:active:%d";
	private static final String LOCK_KEY = "queue:lock:%d";
	private static final long ENTRY_TTL_SEC = 86400L;
	private static final long ACTIVE_TTL_SEC = 300L;
	private static final long LOCK_TTL_SEC = 2L;

	private final StringRedisTemplate redisTemplate;

	public EventQueueRepositoryImpl(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public QueueEntry enqueue(Long eventId, Long memberNo, Integer count) {
		String token = UUID.randomUUID().toString();
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
	public List<QueueEntry> dequeueForProcessing(Long eventId, int batchSize) {
		String queueKey = String.format(QUEUE_KEY, eventId);
		Set<ZSetOperations.TypedTuple<String>> tuples =
			redisTemplate.opsForZSet().popMin(queueKey, batchSize);

		if (tuples == null || tuples.isEmpty()) {
			return List.of();
		}

		List<QueueEntry> entries = new ArrayList<>();
		for (ZSetOperations.TypedTuple<String> tuple : tuples) {
			String token = tuple.getValue();
			if (token == null) continue;

			String entryKey = String.format(ENTRY_KEY, token);
			Map<Object, Object> fields = redisTemplate.opsForHash().entries(entryKey);
			if (fields.isEmpty()) continue;

			Long entryMemberNo = Long.parseLong((String) fields.get("memberNo"));
			Integer entryCount = Integer.parseInt((String) fields.get("count"));
			Long entryEventId = Long.parseLong((String) fields.get("eventId"));
			long score = tuple.getScore() == null ? 0L : tuple.getScore().longValue();

			entries.add(new QueueEntry(token, entryEventId, entryMemberNo, entryCount, score));
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
	public void activateTokens(Long eventId, List<String> tokens) {
		String activeKey = String.format(ACTIVE_KEY, eventId);
		String queueKey = String.format(QUEUE_KEY, eventId);

		redisTemplate.opsForSet().add(activeKey, tokens.toArray(new String[0]));
		redisTemplate.expire(activeKey, Duration.ofSeconds(ACTIVE_TTL_SEC));

		Long remaining = redisTemplate.opsForZSet().zCard(queueKey);
		if (remaining == null || remaining == 0) {
			redisTemplate.opsForSet().remove(ACTIVE_EVENTS, String.valueOf(eventId));
		}
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
