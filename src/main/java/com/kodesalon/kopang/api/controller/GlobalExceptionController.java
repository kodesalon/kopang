package com.kodesalon.kopang.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.kodesalon.kopang.service.exception.NotFoundException;

@RestControllerAdvice
public class GlobalExceptionController {

	@ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
	public ResponseEntity<KopangExceptionResponse> badRequest(RuntimeException e) {
		return ResponseEntity.badRequest()
			.body(new KopangExceptionResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
	}

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<KopangExceptionResponse> notFound(RuntimeException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(new KopangExceptionResponse(e.getMessage(), HttpStatus.NOT_FOUND.value()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<KopangExceptionResponse> internalServerError(Exception e) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(new KopangExceptionResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
	}
}
