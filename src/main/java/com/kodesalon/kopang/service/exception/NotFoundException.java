package com.kodesalon.kopang.domain.exception;

public class NotFoundException extends RuntimeException {

	public NotFoundException(String message) {
		super(message);
	}

	public static NotFoundException stock(Long productNo) {
		return new NotFoundException(String.format("상품 %s 에 대한 재고를 찾을 수 없습니다", productNo));
	}
}
