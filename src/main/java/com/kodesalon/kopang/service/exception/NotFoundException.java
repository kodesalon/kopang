package com.kodesalon.kopang.service.exception;

public class NotFoundException extends RuntimeException {

	public NotFoundException(String message) {
		super(message);
	}

	public static NotFoundException stock(Long productNo) {
		return new NotFoundException(String.format("상품 %s 에 대한 재고를 찾을 수 없습니다", productNo));
	}

	public static NotFoundException product(Long productNo) {
		return new NotFoundException(String.format("상품 %s 를 찾을 수 없습니다", productNo));
	}

	public static NotFoundException order(Long orderNo) {
		return new NotFoundException(String.format("주문 %s 를 찾을 수 없습니다", orderNo));
	}
}
