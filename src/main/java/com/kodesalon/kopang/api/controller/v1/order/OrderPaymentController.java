package com.kodesalon.kopang.api.controller.v1.order;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kodesalon.kopang.api.aop.PreventDuplicateRequest;
import com.kodesalon.kopang.domain.payment.Payment;
import com.kodesalon.kopang.service.payment.PaymentOrchestrator;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderPaymentController {

	private final PaymentOrchestrator paymentOrchestrator;

	public OrderPaymentController(PaymentOrchestrator paymentOrchestrator) {
		this.paymentOrchestrator = paymentOrchestrator;
	}

	@PreventDuplicateRequest(keyExpression = "'payment:' + #memberNo + ':' + #orderNo", ttlSeconds = 5)
	@PostMapping("/{orderNo}/payment")
	public ResponseEntity<PaymentResponse> confirmPayment(
		@RequestParam Long memberNo,
		@PathVariable Long orderNo,
		@RequestBody PaymentRequest request
	) {
		Payment payment = paymentOrchestrator
			.executePayment(request.paymentKey(), orderNo, request.amount(), request.productNo(), request.orderCount());
		return ResponseEntity.ok(PaymentResponse.from(payment));
	}
}
