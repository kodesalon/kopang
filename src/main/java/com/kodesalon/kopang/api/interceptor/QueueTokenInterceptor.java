package com.kodesalon.kopang.api.interceptor;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.kodesalon.kopang.service.queue.EventQueueService;

@Component
public class QueueTokenInterceptor implements HandlerInterceptor {

	private static final String QUEUE_TOKEN_HEADER = "X-Queue-Token";

	private final EventQueueService eventQueueService;

	public QueueTokenInterceptor(EventQueueService eventQueueService) {
		this.eventQueueService = eventQueueService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		String token = request.getHeader(QUEUE_TOKEN_HEADER);
		if (token == null) {
			return true;
		}

		Optional<Long> eventId = eventQueueService.findEventIdByToken(token);
		if (eventId.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return false;
		}

		if (!eventQueueService.isTokenActive(eventId.get(), token)) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return false;
		}

		return true;
	}
}
