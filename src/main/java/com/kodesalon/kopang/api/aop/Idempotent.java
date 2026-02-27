package com.kodesalon.kopang.api.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

	String keyExpression();

	int ttlHours() default 24;

	int processingTimeoutSeconds() default 30;
}