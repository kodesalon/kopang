package com.kodesalon.kopang.domain.queue;

import java.util.UUID;

public record QueueEntry(
	String token,
	Long eventId,
	Long memberNo,
	Integer count,
	long requestedAt
) {

	public static QueueEntry of(Long eventId, Long memberNo, Integer count) {
		return new QueueEntry(
			UUID.randomUUID().toString(),
			eventId,
			memberNo,
			count,
			System.currentTimeMillis()
		);
	}
}
