package com.kodesalon.kopang.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.github.benmanes.caffeine.cache.Caffeine;

@EnableCaching
@Configuration
public class CacheConfig {

	@Primary
	@Bean(name = Caches.Manager.REDIS)
	public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
		ObjectMapper objectMapper = new ObjectMapper();
		PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
			.allowIfSubType(Object.class)
			.build();
		objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
		objectMapper.registerModule(new ParameterNamesModule());
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		RedisCacheConfiguration warehouseConfig = RedisCacheConfiguration.defaultCacheConfig()
			.disableCachingNullValues()
			.entryTtl(Duration.ofHours(1))
			.serializeKeysWith(RedisSerializationContext.SerializationPair
				.fromSerializer(new StringRedisSerializer()))
			.serializeValuesWith(RedisSerializationContext.SerializationPair
				.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));

		return RedisCacheManager.builder(redisConnectionFactory)
			.cacheDefaults(warehouseConfig)
			.build();
	}

	@Bean(name = Caches.Manager.CAFFEINE)
	public CacheManager caffeinCacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager();

		cacheManager.registerCustomCache("product_warehouses",
			Caffeine.newBuilder()
				// [분석 결과 반영]
				// 초기 사이즈: 이벤트 시작과 동시에 트래픽이 몰리므로 리사이징 비용을 줄이기 위해 적당히 확보
				.initialCapacity(100)
				// 최대 사이즈: 프로모션 상품 수(예: 200개)보다 훨씬 넉넉하게 잡음.
				// 메모리 이슈는 없으므로, 'Cache Miss'를 0에 수렴하게 만드는 것이 목표.
				.maximumSize(1000)
				// TTL: 데이터 변경이 거의 없지만, 혹시 모를 운영 이슈 대비 10분 설정
				.expireAfterWrite(10, TimeUnit.MINUTES)
				// 모니터링: 실제 Hit Rate를 보고 튜닝하기 위해 필수
				.recordStats()
				.build()
		);
		return cacheManager;
	}
}
