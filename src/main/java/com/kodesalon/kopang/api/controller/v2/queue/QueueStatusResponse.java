package com.kodesalon.kopang.api.controller.v2.queue;

import com.kodesalon.kopang.service.queue.QueueStatusResult;

public record QueueStatusResponse(
	String status,
	Long position
) {

	public static QueueStatusResponse of(QueueStatusResult result) {
		return new QueueStatusResponse(result.status().name(), result.position());
	}
}
