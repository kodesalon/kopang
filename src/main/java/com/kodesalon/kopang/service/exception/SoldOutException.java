package com.kodesalon.kopang.service.exception;

public class SoldOutException extends RuntimeException {

	public SoldOutException(String message) {
		super(message);
	}

	public static SoldOutException warehouse(Long productNo) {
		return new SoldOutException(String.format("상품 {%d}이 모든 물류 센터 품절되었습니다.", productNo));
	}
}
