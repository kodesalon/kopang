package com.kodesalon.kopang.api.controller.v1.order;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kodesalon.kopang.api.aop.Idempotent;
import com.kodesalon.kopang.service.purchase.PurchaseOrchestrator;
import com.kodesalon.kopang.service.purchase.ReservationOrderResult;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

	private final PurchaseOrchestrator purchaseOrchestrator;

	public OrderController(PurchaseOrchestrator purchaseOrchestrator) {
		this.purchaseOrchestrator = purchaseOrchestrator;
	}

	@Idempotent(keyExpression = "#idempotencyKey", processingTimeoutSeconds = 10)
	@PostMapping
	public ResponseEntity<ReservationOrderResponse> createReservationOrder(
		@RequestHeader("Idempotency-Key") String idempotencyKey,
		@RequestParam Long memberNo,
		@RequestBody CreateReservationOrderRequest request
	) {
		ReservationOrderResult result = purchaseOrchestrator.reserve(memberNo, request.productNo(), request.count());
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ReservationOrderResponse.of(result.order(), result.quantity()));
	}
}