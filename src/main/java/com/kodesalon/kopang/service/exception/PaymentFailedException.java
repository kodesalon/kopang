package com.kodesalon.kopang.service.exception;

public class PaymentFailedException extends RuntimeException {

	public PaymentFailedException(String message) {
		super(message);
	}

	public static PaymentFailedException aborted(String paymentKey, Long orderNo, String message) {
		return new PaymentFailedException(String.format(
			"결제에 실패했습니다. payment key : %s , orderNo : %d , failure message : %s",
			paymentKey, orderNo, message
		));
	}

	public static PaymentFailedException expired(String paymentKey, Long orderNo, String message) {
		return new PaymentFailedException(String.format(
			"결제 유효시간이 만료되었습니다. payment key : %s , orderNo : %d , failure message : %s",
			paymentKey, orderNo, message
		));
	}

	public static PaymentFailedException invalid(String paymentKey, Long orderNo, String status) {
		return new PaymentFailedException(String.format(
			"지원하지 않는 결제 상태입니다. payment key : %s , orderNo : %d , invalid status : %s",
			paymentKey, orderNo, status
		));
	}
}
