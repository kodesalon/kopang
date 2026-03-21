package com.kodesalon.kopang.service.queue;

import com.kodesalon.kopang.domain.queue.QueueStatus;

public record QueueStatusResult(
	QueueStatus status,
	Long position
) {
}
