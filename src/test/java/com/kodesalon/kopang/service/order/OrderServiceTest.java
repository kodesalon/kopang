package com.kodesalon.kopang.service.order;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
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
import com.kodesalon.kopang.domain.order.event.OrderStockEvent;
import com.kodesalon.kopang.domain.order.event.OrderStockEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	private @Mock OrderRepository orderRepository;
	private @Mock OrderStockEventPublisher eventPublisher;
	private @InjectMocks OrderService orderService;

	@DisplayName("유효한 주문 정보가 주어질 때, 주문 생성을 요청하면, PENDING 상태의 주문이 저장소에 등록되고 OutBox 이벤트가 발행된다.")
	@Test
	void createOrderPending_ValidInput_RegistersOrderAndPublishesEvent() {
		// given
		Long memberNo = 1L;
		Long productNo = 1L;
		Long warehouseNo = 1L;
		Integer count = 2;
		BigDecimal productPrice = new BigDecimal("1000");
		Order registeredOrder = Order.createPending(memberNo, productNo, warehouseNo, count, productPrice);
		given(orderRepository.register(any(Order.class))).willReturn(registeredOrder);

		// when
		Order result = orderService.createOrderPending(memberNo, productNo, warehouseNo, count, productPrice);

		// then
		assertAll(
			() -> assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING),
			() -> assertThat(result.getMemberNo()).isEqualTo(memberNo),
			() -> assertThat(result.getTotalPrice()).isEqualTo(new Money(2000L)),
			() -> assertThat(result.getProducts().size()).isEqualTo(1)
		);
		verify(orderRepository, times(1)).register(any(Order.class));
		verify(eventPublisher, times(1)).createOrderPending(any(OrderStockEvent.class));
	}
}