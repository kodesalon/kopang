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
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.github.benmanes.caffeine.cache.Caffeine;

@EnableCaching
@Configuration
public class CacheConfig {

	/**
	 * Redis Cache Config
	 */
	@Primary
	@Bean(name = Caches.Manager.REDIS)
	public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
		ObjectMapper objectMapper = new ObjectMapper();
		PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
			.allowIfSubType(Object.class)
			.build();
		RecordSupportingTypeResolver typeResolver = new RecordSupportingTypeResolver(ObjectMapper.DefaultTyping.NON_FINAL, ptv);
		StdTypeResolverBuilder initializedResolver = typeResolver.init(JsonTypeInfo.Id.CLASS, null);
		initializedResolver = initializedResolver.inclusion(JsonTypeInfo.As.PROPERTY);
		objectMapper.setDefaultTyping(initializedResolver);

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

	public static class RecordSupportingTypeResolver extends ObjectMapper.DefaultTypeResolverBuilder {

		public RecordSupportingTypeResolver(ObjectMapper.DefaultTyping t, PolymorphicTypeValidator ptv) {
			super(t, ptv);
		}

		@Override
		public boolean useForType(JavaType t) {
			boolean isRecord = t.getRawClass().isRecord();
			boolean superResult = super.useForType(t);

			if (isRecord) {
				return true;
			}
			return superResult;
		}
	}

	/**
	 *  Caffeine Cache Config
	 */
	@Bean(name = Caches.Manager.CAFFEINE)
	public CacheManager caffeinCacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager();

		cacheManager.registerCustomCache(Caches.Name.PRODUCT_WAREHOUSES,
			Caffeine.newBuilder()
				.initialCapacity(100)
				.maximumSize(1000)
				.expireAfterWrite(10, TimeUnit.MINUTES)
				.recordStats()
				.build()
		);
		cacheManager.registerCustomCache(Caches.Name.PRODUCT,
			Caffeine.newBuilder()
				.initialCapacity(50)
				.maximumSize(100)
				.expireAfterWrite(10, TimeUnit.MINUTES)
				.recordStats()
				.build()
		);
		// Caffeine TTL은 Redis TTL(@PreventDuplicateRequest.ttlSeconds 기본값 3s)보다
		// 작거나 같아야 합니다. Caffeine 만료 후 Redis 키가 여전히 존재하면 Redis에서
		// 차단되며, 이는 의도된 동작입니다 (더 안전한 방향). 반대로 Caffeine TTL이
		// Redis TTL을 초과하면 Redis 만료 이후에도 같은 노드에서만 불필요하게 차단됩니다.
		cacheManager.registerCustomCache(Caches.Name.DUPLICATE_REQUEST_GUARD,
			Caffeine.newBuilder()
				.initialCapacity(1000)
				.maximumSize(10000)
				.expireAfterWrite(3, TimeUnit.SECONDS)
				.build()
		);
		return cacheManager;
	}
}
