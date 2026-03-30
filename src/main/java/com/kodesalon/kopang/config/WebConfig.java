package com.kodesalon.kopang.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.kodesalon.kopang.api.interceptor.QueueTokenInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final QueueTokenInterceptor queueTokenInterceptor;

	public WebConfig(QueueTokenInterceptor queueTokenInterceptor) {
		this.queueTokenInterceptor = queueTokenInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(queueTokenInterceptor)
			.addPathPatterns("/api/v1/orders");
	}
}
