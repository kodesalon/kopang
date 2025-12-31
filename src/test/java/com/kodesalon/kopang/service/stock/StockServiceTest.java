package com.kodesalon.kopang.service.stock;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kodesalon.kopang.domain.stock.StockQuantity;
import com.kodesalon.kopang.service.exception.NotFoundException;
import com.kodesalon.kopang.domain.stock.Stock;
import com.kodesalon.kopang.domain.stock.StockRepository;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

	private @Mock StockRepository stockRepository;
	private @InjectMocks StockService stockService;

	@DisplayName("재고 감소 시 상품이 없으면 NotFoundException 예외가 발생한다")
	@Test
	void decrease_fail_notFound() {
		when(stockRepository.findByProductNo(anyLong()))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> stockService.decrease(1L, 1))
			.isInstanceOf(NotFoundException.class)
			.hasMessage(NotFoundException.stock(1L).getMessage());
	}

	@DisplayName("재고 감소 성공 시, 기존 재고의 -1 한 재고가 반환된다")
	@Test
	void decrease_success() {
		Long productNo = 1L;
		Stock stock = new Stock(1L, productNo, StockQuantity.from(100));
		given(stockRepository.findByProductNo(productNo))
			.willReturn(Optional.of(stock));

		Stock result = stockService.decrease(productNo, 1);

		assertThat(result.getQuantity()).isEqualTo(99);
	}
}