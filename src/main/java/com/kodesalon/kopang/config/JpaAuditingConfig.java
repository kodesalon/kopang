package com.kodesalon.kopang.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * {@code @CreatedDate}, {@code @LastModifiedDate}를 채운다.
 * 주문 시각(orders.ordered_at)과 이벤트 시각(order_stock_event.created_at)이 비어 있으면
 * 만료 취소·대사·재발행 스케줄러가 대상을 찾지 못한다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
