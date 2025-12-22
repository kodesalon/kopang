package com.kodesalon.kopang.storage.stock;

import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import com.kodesalon.kopang.domain.stock.StockReservationRepository;

@Repository
public class RedisStockReservationRepositoryImpl implements StockReservationRepository {

	private static final String PRODUCT_STOCK_KEY_FORMAT = "product:%d:stock";

	private final RedisTemplate<String, String> redisTemplate;
	private final DefaultRedisScript<Long> decreaseStockScript;

	public RedisStockReservationRepositoryImpl(RedisTemplate<String, String> redisTemplate) {
		this.redisTemplate = redisTemplate;
		this.decreaseStockScript = new DefaultRedisScript<>();
		this.decreaseStockScript.setLocation(new ClassPathResource("redis/decrease_stock.lua"));
		this.decreaseStockScript.setResultType(Long.class);
	}

	@Override
	public boolean decreaseStock(Long productNo, Integer count) {
		return redisTemplate.execute(
			decreaseStockScript,
			List.of(String.format(PRODUCT_STOCK_KEY_FORMAT, productNo)),
			String.valueOf(count)
		) >= 0;
	}

	@Override
	public void increaseStock(Long productNo, Integer count) {
		redisTemplate.opsForValue().increment(String.format(PRODUCT_STOCK_KEY_FORMAT, productNo), count);
	}
}
