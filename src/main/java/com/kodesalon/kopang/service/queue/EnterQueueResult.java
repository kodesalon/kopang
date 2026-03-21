package com.kodesalon.kopang.service.queue;

public record EnterQueueResult(
	String token,
	long position,
	long estimatedWaitMs
) {
}
