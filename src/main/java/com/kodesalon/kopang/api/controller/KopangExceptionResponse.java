package com.kodesalon.kopang.api.controller;

public record KopangExceptionResponse(
	String message,
	int code
) {
}
