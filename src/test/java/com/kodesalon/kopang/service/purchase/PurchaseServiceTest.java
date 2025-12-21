package com.kodesalon.kopang.service.purchase;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.domain.order.OrderFixture;
import com.kodesalon.kopang.domain.stock.Stock;
import com.kodesalon.kopang.service.order.OrderService;
import com.kodesalon.kopang.service.stock.StockService;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

	private @Mock StockService stockService;
	private @Mock OrderService orderService;
	private @InjectMocks PurchaseService purchaseService;

	@DisplayName("상품 예약을 수행하면 재고가 감소하고 주문이 생성된다")
	@Test
	void reservation_success() {
		Long productNo = 1L;
		Long memberNo = 1L;
		Integer count = 1;
		Stock stock = new Stock(1L, 1L, 100);
		Order order = OrderFixture.PENDING_ORDER;
		given(stockService.decrease(productNo, count)).willReturn(stock);
		given(orderService.createOrder(memberNo, productNo, count)).willReturn(order);

		ReservationOrderResult result = purchaseService.reservation(memberNo, productNo, count);

		assertAll(
			() -> assertThat(result).isNotNull(),
			() -> assertThat(result.stock()).isEqualTo(stock),
			() -> assertThat(result.order()).isEqualTo(order),
			() -> verify(stockService).decrease(productNo, count),
			() -> verify(orderService).createOrder(memberNo, productNo, count)
		);
	}
}