package com.kodesalon.kopang.api.controller.v1.order;

public record CreateReservationOrderRequest(
	Long productNo,
	Integer count
) {
}
