package com.kodesalon.kopang.service.purchase;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kodesalon.kopang.domain.Address;
import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.domain.order.Orders;
import com.kodesalon.kopang.domain.stock.StockQuantity;
import com.kodesalon.kopang.domain.warehouse.Warehouse;
import com.kodesalon.kopang.domain.warehouse.Warehouses;
import com.kodesalon.kopang.service.exception.SoldOutException;
import com.kodesalon.kopang.service.member.MemberAddressService;
import com.kodesalon.kopang.service.order.OrderService;
import com.kodesalon.kopang.service.stock.StockReservationService;
import com.kodesalon.kopang.service.warehouse.WarehouseService;

@Component
public class PurchaseOrchestrator {

	private final MemberAddressService memberAddressService;
	private final WarehouseService warehouseService;
	private final StockReservationService stockReservationService;
	private final OrderService orderService;

	public PurchaseOrchestrator(
		MemberAddressService memberAddressService,
		WarehouseService warehouseService,
		StockReservationService stockReservationService,
		OrderService orderService
	) {
		this.memberAddressService = memberAddressService;
		this.warehouseService = warehouseService;
		this.stockReservationService = stockReservationService;
		this.orderService = orderService;
	}

	public ReservationOrderResult reserve(Long memberNo, Long productNo, Integer count) {
		Address memberAddress = memberAddressService.findDefaultMemberAddress(memberNo).getAddress();
		Warehouses warehouses = warehouseService
			.findWarehousesForProduct(productNo)
			.sortedByDistance(memberAddress);

		Warehouse allocatedWarehouse = null;
		StockQuantity finalStock = null;
		for (Warehouse warehouse : warehouses) {
			Optional<StockQuantity> sq = stockReservationService.decrease(warehouse.getNo(), productNo, count);
			if (sq.isPresent()) {
				finalStock = sq.get();
				allocatedWarehouse = warehouse;
				break;
			}
		}
		if (allocatedWarehouse == null) {
			throw SoldOutException.warehouse(productNo);
		}

		try {
			Order order = orderService.createOrderPending(memberNo, productNo, allocatedWarehouse.getNo(), count);
			return new ReservationOrderResult(finalStock, order);
		} catch (Exception e) {
			stockReservationService.increase(allocatedWarehouse.getNo(), productNo, count);
			throw e;
		}
	}

	public void cancel(Long orderNo, Long productNo, Integer count) {
		Order cancelled = orderService.cancelOrder(orderNo);
		stockReservationService.increase(cancelled.getEventProduct().getWarehouseNo(), productNo, count);
	}

	public void cancelInBatch(Orders expiredOrders) {
		orderService.cancelExpiredOrders(expiredOrders.getAllIds());
		stockReservationService.restoreInBatch(expiredOrders.groupByStockKey());
	}
}
