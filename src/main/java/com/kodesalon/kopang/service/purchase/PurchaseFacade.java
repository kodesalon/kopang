package com.kodesalon.kopang.service.purchase;

import org.springframework.stereotype.Component;

import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.domain.stock.StockQuantity;
import com.kodesalon.kopang.service.order.OrderService;
import com.kodesalon.kopang.service.stock.StockReservationService;

@Component
public class PurchaseFacade {

	private final StockReservationService stockReservationService;
	private final OrderService orderService;

	public PurchaseFacade(StockReservationService stockReservationService, OrderService orderService) {
		this.stockReservationService = stockReservationService;
		this.orderService = orderService;
	}

	public ReservationOrderResult reserve(Long memberNo, Long productNo, Integer count) {
		StockQuantity quantity = stockReservationService.decrease(productNo, count);
		try {
			Order order = orderService.createOrderPending(memberNo, productNo, count);
			return new ReservationOrderResult(quantity, order);
		} catch (Exception e) {
			stockReservationService.increase(productNo, count);
			throw e;
		}
	}
}
