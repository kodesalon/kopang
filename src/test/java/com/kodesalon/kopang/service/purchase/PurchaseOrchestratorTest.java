package com.kodesalon.kopang.service.purchase;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kodesalon.kopang.domain.Address;
import com.kodesalon.kopang.domain.Coordinate;
import com.kodesalon.kopang.domain.order.Order;
import com.kodesalon.kopang.domain.order.OrderStatus;
import com.kodesalon.kopang.domain.product.Product;
import com.kodesalon.kopang.domain.stock.StockQuantity;
import com.kodesalon.kopang.domain.warehouse.Warehouse;
import com.kodesalon.kopang.domain.warehouse.WarehouseRegion;
import com.kodesalon.kopang.domain.warehouse.Warehouses;
import com.kodesalon.kopang.service.exception.SoldOutException;
import com.kodesalon.kopang.service.member.MemberAddressCacheService;
import com.kodesalon.kopang.service.order.OrderService;
import com.kodesalon.kopang.service.product.ProductCacheService;
import com.kodesalon.kopang.service.stock.StockReservationService;
import com.kodesalon.kopang.service.warehouse.WarehouseCacheService;

@ExtendWith(MockitoExtension.class)
class PurchaseOrchestratorTest {

	private @Mock MemberAddressCacheService memberAddressCacheService;
	private @Mock WarehouseCacheService warehouseCacheService;
	private @Mock StockReservationService stockReservationService;
	private @Mock ProductCacheService productCacheService;
	private @Mock OrderService orderService;
	private @InjectMocks PurchaseOrchestrator purchaseOrchestrator;

	// 서울 강남(37.5, 127.0) 기준 — 송파(37.51, 127.01)가 대전(36.35, 127.3)보다 가까움
	private static final Address MEMBER_ADDRESS =
		new Address("06164", "서울시 강남구 테헤란로", null, new Coordinate(37.5, 127.0));
	private static final Warehouse SEOUL_WAREHOUSE =
		new Warehouse(1L, "서울 창고", WarehouseRegion.SEOUL,
			new Address("05717", "서울시 송파구 올림픽로", null, new Coordinate(37.51, 127.01)));
	private static final Warehouse DAEJEON_WAREHOUSE =
		new Warehouse(2L, "대전 창고", WarehouseRegion.DAEJEON,
			new Address("34141", "대전시 유성구 대학로", null, new Coordinate(36.35, 127.3)));
	private static final Product PRODUCT =
		new Product(1L, "테스트 상품", "상품 설명", BigDecimal.valueOf(10000));

	@Nested
	@DisplayName("reserve — 선착순 주문 예약")
	class Reserve {

		@DisplayName("가장 가까운 창고에 재고가 있을 때, 주문을 예약하면, 해당 창고에서만 재고를 차감하고 PENDING 주문을 반환한다.")
		@Test
		void reserve_FirstWarehouseHasStock_DecreasesOnlyFirstAndReturnsPendingOrder() {
			// given
			StockQuantity remaining = StockQuantity.from(98);
			Order expectedOrder = Order.createPending(1L, 1L, SEOUL_WAREHOUSE.getNo(), 2, PRODUCT.getPrice());
			given(memberAddressCacheService.getDefaultMemberAddress(1L)).willReturn(MEMBER_ADDRESS);
			given(warehouseCacheService.getWarehousesForProduct(1L))
				.willReturn(new Warehouses(List.of(SEOUL_WAREHOUSE, DAEJEON_WAREHOUSE)));
			given(stockReservationService.decrease(SEOUL_WAREHOUSE.getNo(), 1L, 2))
				.willReturn(Optional.of(remaining));
			given(productCacheService.getProduct(1L)).willReturn(PRODUCT);
			given(orderService.createOrderPending(1L, 1L, SEOUL_WAREHOUSE.getNo(), 2, PRODUCT.getPrice()))
				.willReturn(expectedOrder);

			// when
			ReservationOrderResult result = purchaseOrchestrator.reserve(1L, 1L, 2);

			// then
			assertAll(
				() -> assertThat(result.order().getStatus()).isEqualTo(OrderStatus.PENDING),
				() -> assertThat(result.quantity()).isEqualTo(remaining)
			);
			verify(stockReservationService, times(1)).decrease(SEOUL_WAREHOUSE.getNo(), 1L, 2);
			verify(stockReservationService, never()).decrease(DAEJEON_WAREHOUSE.getNo(), 1L, 2);
		}

		@DisplayName("가장 가까운 창고가 품절일 때, 주문을 예약하면, 다음으로 가까운 창고에서 재고를 차감하고 PENDING 주문을 반환한다.")
		@Test
		void reserve_FirstWarehouseSoldOut_DecreasesSecondWarehouseAndReturnsPendingOrder() {
			// given
			StockQuantity remaining = StockQuantity.from(50);
			Order expectedOrder = Order.createPending(1L, 1L, DAEJEON_WAREHOUSE.getNo(), 2, PRODUCT.getPrice());
			given(memberAddressCacheService.getDefaultMemberAddress(1L)).willReturn(MEMBER_ADDRESS);
			given(warehouseCacheService.getWarehousesForProduct(1L))
				.willReturn(new Warehouses(List.of(SEOUL_WAREHOUSE, DAEJEON_WAREHOUSE)));
			given(stockReservationService.decrease(SEOUL_WAREHOUSE.getNo(), 1L, 2)).willReturn(Optional.empty());
			given(stockReservationService.decrease(DAEJEON_WAREHOUSE.getNo(), 1L, 2))
				.willReturn(Optional.of(remaining));
			given(productCacheService.getProduct(1L)).willReturn(PRODUCT);
			given(orderService.createOrderPending(1L, 1L, DAEJEON_WAREHOUSE.getNo(), 2, PRODUCT.getPrice()))
				.willReturn(expectedOrder);

			// when
			ReservationOrderResult result = purchaseOrchestrator.reserve(1L, 1L, 2);

			// then
			assertAll(
				() -> assertThat(result.order().getStatus()).isEqualTo(OrderStatus.PENDING),
				() -> assertThat(result.quantity()).isEqualTo(remaining)
			);
			verify(stockReservationService, times(1)).decrease(SEOUL_WAREHOUSE.getNo(), 1L, 2);
			verify(stockReservationService, times(1)).decrease(DAEJEON_WAREHOUSE.getNo(), 1L, 2);
		}

		@DisplayName("모든 창고가 품절일 때, 주문을 예약하면, SoldOutException이 발생한다.")
		@Test
		void reserve_AllWarehousesSoldOut_ThrowsSoldOutException() {
			// given
			given(memberAddressCacheService.getDefaultMemberAddress(1L)).willReturn(MEMBER_ADDRESS);
			given(warehouseCacheService.getWarehousesForProduct(1L))
				.willReturn(new Warehouses(List.of(SEOUL_WAREHOUSE, DAEJEON_WAREHOUSE)));
			given(stockReservationService.decrease(any(Long.class), eq(1L), eq(2)))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> purchaseOrchestrator.reserve(1L, 1L, 2))
				.isInstanceOf(SoldOutException.class);
			verify(stockReservationService, times(1)).decrease(SEOUL_WAREHOUSE.getNo(), 1L, 2);
			verify(stockReservationService, times(1)).decrease(DAEJEON_WAREHOUSE.getNo(), 1L, 2);
		}

		@DisplayName("재고 차감 후 주문 생성이 실패할 때, 차감한 Redis 재고를 복구(보상 트랜잭션)한다.")
		@Test
		void reserve_OrderCreationFails_IncreasesStockForCompensation() {
			// given
			given(memberAddressCacheService.getDefaultMemberAddress(1L)).willReturn(MEMBER_ADDRESS);
			given(warehouseCacheService.getWarehousesForProduct(1L))
				.willReturn(new Warehouses(List.of(SEOUL_WAREHOUSE)));
			given(stockReservationService.decrease(SEOUL_WAREHOUSE.getNo(), 1L, 2))
				.willReturn(Optional.of(StockQuantity.from(98)));
			given(productCacheService.getProduct(1L)).willReturn(PRODUCT);
			given(orderService.createOrderPending(any(), any(), any(), any(), any()))
				.willThrow(new RuntimeException("DB 장애"));

			// when & then
			assertThatThrownBy(() -> purchaseOrchestrator.reserve(1L, 1L, 2))
				.isInstanceOf(RuntimeException.class);
			verify(stockReservationService, times(1)).increase(SEOUL_WAREHOUSE.getNo(), 1L, 2);
		}
	}
}