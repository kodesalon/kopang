package com.kodesalon.kopang.api.controller.v2.queue;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kodesalon.kopang.service.queue.EnterQueueResult;
import com.kodesalon.kopang.service.queue.EventQueueOrchestrator;
import com.kodesalon.kopang.service.queue.QueueStatusResult;

@RestController
@RequestMapping("/api/v2/events/{eventId}/queue")
public class EventQueueController {

	private final EventQueueOrchestrator eventQueueOrchestrator;

	public EventQueueController(EventQueueOrchestrator eventQueueOrchestrator) {
		this.eventQueueOrchestrator = eventQueueOrchestrator;
	}

	@PostMapping
	public ResponseEntity<EnterQueueResponse> enterQueue(
		@PathVariable Long eventId,
		@RequestParam Long memberNo,
		@RequestBody EnterQueueRequest request
	) {
		EnterQueueResult result = eventQueueOrchestrator.enqueue(eventId, memberNo, request.count());
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(EnterQueueResponse.of(result));
	}

	@GetMapping("/{token}/status")
	public ResponseEntity<QueueStatusResponse> getQueueStatus(
		@PathVariable Long eventId,
		@PathVariable String token
	) {
		QueueStatusResult result = eventQueueOrchestrator.getStatus(eventId, token);
		return ResponseEntity.ok(QueueStatusResponse.of(result));
	}
}
