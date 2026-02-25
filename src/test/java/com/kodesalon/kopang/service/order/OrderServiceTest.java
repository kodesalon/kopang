package com.kodesalon.kopang.service.order;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;

import java.math.BigDecimal;

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
import com.kodesalon.kopang.domain.order.event.OrderStockEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	private @Mock OrderRepository orderRepository;
	private @Mock OrderStockEventPublisher orderStockEventPublisher;
	private @InjectMocks OrderService orderService;

	@DisplayName("상품 가격과 수량이 주어지면 주문 생성에 성공하고 저장소에 등록한다")
	@Test
	void createOrder_success() {
		Long memberNo = 1L;
		Long productNo = 1L;
		Long warehouseNo = 1L;
		Integer count = 2;
		BigDecimal productPrice = new BigDecimal(1000);
		Order expectedResult = Order.createPending(memberNo, productNo, warehouseNo, count, productPrice);
		given(orderRepository.register(any(Order.class)))
			.willReturn(expectedResult);

		Order result = orderService.createOrderPending(memberNo, productNo, warehouseNo, count, productPrice);

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

	@DisplayName("존재하지 않는 주문번호로 결제 취소를 요청하면 NotFoundException 이 발생한다")
	@Test
	void cancelOrder_fail_orderNotFound() {
		Long nonExistentOrderNo = Long.MAX_VALUE;
		given(orderRepository.findByOrderNo(nonExistentOrderNo))
			.willReturn(java.util.Optional.empty());

		assertThatThrownBy(() -> orderService.cancelOrder(nonExistentOrderNo))
			.isInstanceOf(com.kodesalon.kopang.service.exception.NotFoundException.class)
			.hasMessage(com.kodesalon.kopang.service.exception.NotFoundException.order(nonExistentOrderNo).getMessage());
	}
}