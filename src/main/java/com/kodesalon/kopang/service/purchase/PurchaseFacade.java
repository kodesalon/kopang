package com.kodesalon.kopang.service.purchase;

import org.springframework.stereotype.Component;

import com.kodesalon.kopang.service.stock.StockReservationService;

@Component
public class PurchaseFacade {

	private final StockReservationService stockReservationService;
	private final PurchaseService purchaseService;

	public PurchaseFacade(StockReservationService stockReservationService, PurchaseService purchaseService) {
		this.stockReservationService = stockReservationService;
		this.purchaseService = purchaseService;
	}

	public ReservationOrderResult reserve(Long memberNo, Long productNo, Integer count) {
		stockReservationService.decrease(productNo, count);
		try {
			return purchaseService.reservation(memberNo, productNo, count);
		} catch (Exception e) {
			stockReservationService.increase(productNo, count);
			throw e;
		}
	}
}
