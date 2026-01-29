package com.kodesalon.kopang.service.purchase;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kodesalon.kopang.domain.Address;
import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.domain.order.Orders;
import com.kodesalon.kopang.domain.product.Product;
import com.kodesalon.kopang.domain.stock.StockQuantity;
import com.kodesalon.kopang.domain.warehouse.Warehouse;
import com.kodesalon.kopang.domain.warehouse.Warehouses;
import com.kodesalon.kopang.service.exception.SoldOutException;
import com.kodesalon.kopang.service.member.MemberAddressCacheService;
import com.kodesalon.kopang.service.order.OrderService;
import com.kodesalon.kopang.service.product.ProductCacheService;
import com.kodesalon.kopang.service.stock.StockReservationService;
import com.kodesalon.kopang.service.warehouse.WarehouseCacheService;

@Component
public class PurchaseOrchestrator {

	private final MemberAddressCacheService memberAddressCacheService;
	private final WarehouseCacheService warehouseCacheService;
	private final StockReservationService stockReservationService;
	private final ProductCacheService productCacheService;
	private final OrderService orderService;

	public PurchaseOrchestrator(
		MemberAddressCacheService memberAddressCacheService,
		WarehouseCacheService warehouseCacheService,
		StockReservationService stockReservationService,
		ProductCacheService productCacheService,
		OrderService orderService
	) {
		this.memberAddressCacheService = memberAddressCacheService;
		this.warehouseCacheService = warehouseCacheService;
		this.stockReservationService = stockReservationService;
		this.productCacheService = productCacheService;
		this.orderService = orderService;
	}

	public ReservationOrderResult reserve(Long memberNo, Long productNo, Integer count) {
		Address memberAddress = memberAddressCacheService.getDefaultMemberAddress(memberNo);
		Warehouses warehouses = warehouseCacheService
			.getWarehousesForProduct(productNo)
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

		Product product = productCacheService.getProduct(productNo);
		try {
			Order order = orderService.createOrderPending(memberNo, productNo, allocatedWarehouse.getNo(), count, product.getPrice());
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
