package com.kodesalon.kopang.domain.order;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Orders implements Iterable<Order> {

	private final List<Order> values;

	public Orders(List<Order> values) {
		this.values = values;
	}

	public boolean isEmpty() {
		return values.isEmpty();
	}

	public List<Long> getAllIds() {
		return values.stream()
			.map(Order::getNo)
			.toList();
	}

	public Map<StockKey, Integer> groupByStockKey() {
		return values.stream()
			.map(Order::getEventProduct)
			.collect(Collectors.toMap(
				op -> new StockKey(op.getProductNo(), op.getWarehouseNo()),
				OrderProduct::getCount,
				Integer::sum // 같은 창고/같은 상품이면 더하기
			));
	}

	@Override
	public Iterator<Order> iterator() {
		return values.iterator();
	}

	/**
	 * 재고를 식별하는 Key Record
	 * - Orders의 집계 결과임을 명시
	 */
	public record StockKey(Long productNo, Long warehouseNo) {
	}
}
