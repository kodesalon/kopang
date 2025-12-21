package com.kodesalon.kopang.api.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;

import com.kodesalon.kopang.KopangApplication;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ActiveProfiles("test")
@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = KopangApplication.class,
	properties = {"spring.profiles.active=test"}
)
@TestExecutionListeners(
	value = {AcceptanceTestExecutionListener.class},
	mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
public @interface AcceptanceTest {
	@AliasFor("setUpScripts")
	String[] value() default {};

	@AliasFor("value")
	String[] setUpScripts() default {};
}
