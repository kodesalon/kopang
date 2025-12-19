package com.kodesalon.kopang.service.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.domain.order.OrderRepository;
import com.kodesalon.kopang.domain.product.Product;
import com.kodesalon.kopang.domain.product.ProductRepository;
import com.kodesalon.kopang.service.exception.NotFoundException;

@Service
public class OrderService {

	private final ProductRepository productRepository;
	private final OrderRepository orderRepository;

	public OrderService(ProductRepository productRepository, OrderRepository orderRepository) {
		this.productRepository = productRepository;
		this.orderRepository = orderRepository;
	}

	@Transactional
	public Order createOrderPending(Long memberNo, Long productNo, Integer count) {
		Product product = productRepository.findByProductNo(productNo)
			.orElseThrow(() -> NotFoundException.product(productNo));
		Order pendingOrder = Order.createPending(memberNo, productNo, count, product.getPrice());
		orderRepository.register(pendingOrder);
		return pendingOrder;
	}
}
