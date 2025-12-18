package com.kodesalon.kopang.service.purchase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.domain.stock.Stock;
import com.kodesalon.kopang.service.order.OrderService;
import com.kodesalon.kopang.service.stock.StockService;

@Service
public class PurchaseService {

	private final StockService stockService;
	private final OrderService orderService;

	public PurchaseService(StockService stockService, OrderService orderService) {
		this.stockService = stockService;
		this.orderService = orderService;
	}

	@Transactional
	public ReservationOrderResult reservation(Long memberNo, Long productNo, Integer count) {
		Stock stock = stockService.decrease(productNo);
		Order order = orderService.createOrder(memberNo, productNo, count);
		return new ReservationOrderResult(stock, order);
	}
}
