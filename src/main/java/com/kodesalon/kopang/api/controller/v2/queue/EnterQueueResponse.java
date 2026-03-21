package com.kodesalon.kopang.api.controller.v2.queue;

import com.kodesalon.kopang.service.queue.EnterQueueResult;

public record EnterQueueResponse(
	String token,
	long position,
	long estimatedWaitMs
) {

	public static EnterQueueResponse of(EnterQueueResult result) {
		return new EnterQueueResponse(result.token(), result.position(), result.estimatedWaitMs());
	}
}
