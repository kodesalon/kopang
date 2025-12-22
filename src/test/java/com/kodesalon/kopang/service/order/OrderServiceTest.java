package com.kodesalon.kopang.service.order;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kodesalon.kopang.domain.order.Money;
import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.domain.order.OrderRepository;
import com.kodesalon.kopang.domain.order.OrderStatus;
import com.kodesalon.kopang.domain.product.Product;
import com.kodesalon.kopang.domain.product.ProductRepository;
import com.kodesalon.kopang.service.exception.NotFoundException;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	private @Mock ProductRepository productRepository;
	private @Mock OrderRepository orderRepository;
	private @InjectMocks OrderService orderService;

	@DisplayName("상품이 존재하면 주문 생성에 성공하고 저장소에 등록한다")
	@Test
	void createOrder_success() {
		Long memberNo = 1L;
		Long productNo = 1L;
		Integer count = 2;
		BigDecimal productPrice = new BigDecimal(1000);
		Product product = new Product(productNo, "테스트상품 이름", "테스트상품 설명", productPrice);
		given(productRepository.findByProductNo(productNo))
			.willReturn(Optional.of(product));

		Order result = orderService.createOrderPending(memberNo, productNo, count);

		Money totalPrice = new Money(count.longValue() * productPrice.longValue());
		assertAll(
			() -> assertThat(result).isNotNull(),
			() -> assertThat(result.getMemberNo()).isEqualTo(memberNo),
			() -> assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING),
			() -> assertThat(result.getTotalPrice()).isEqualTo(totalPrice),
			() -> assertThat(result.getProducts().size()).isEqualTo(1),
			() -> verify(orderRepository, times(1)).register(any(Order.class))
		);
	}

	@DisplayName("상품이 존재하지 않으면 NotFoundException 예외가 발생한다")
	@Test
	void createOrder_fail_productNotFound() {
		Long productNo = 1L;
		given(productRepository.findByProductNo(productNo))
			.willReturn(Optional.empty());

		assertAll(
			() -> assertThatThrownBy(() -> orderService.createOrderPending(1L, productNo, 1))
				.isInstanceOf(NotFoundException.class)
				.hasMessage(NotFoundException.product(productNo).getMessage()),
			() -> verify(orderRepository, times(0)).register(any())
		);
	}
}