package com.kodesalon.kopang.api.support;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.RestAssured;

public class AcceptanceTestExecutionListener extends AbstractTestExecutionListener {

	@Override
	public void beforeTestClass(TestContext testContext) {
		Integer serverPort = testContext.getApplicationContext()
			.getEnvironment()
			.getProperty("local.server.port", Integer.class);
		if (serverPort == null) {
			throw new IllegalStateException("localServerPort cannot be null");
		}
		RestAssured.port = serverPort;
	}

	@Override
	public void beforeTestMethod(TestContext testContext) {
		JdbcTemplate jdbcTemplate = testContext.getApplicationContext().getBean(JdbcTemplate.class);
		ObjectMapper objectMapper = testContext.getApplicationContext().getBean(ObjectMapper.class);

		TestContextAnnotationUtils.getMergedRepeatableAnnotations(testContext.getTestClass(), AcceptanceTest.class)
			.stream()
			.map(AcceptanceTest::setUpScripts)
			.forEach(files ->
				Arrays.stream(files)
					.forEach(file ->
						setUpDatabase(jdbcTemplate, objectMapper, file)
					)
			);
	}

	private void setUpDatabase(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, String filePath) {
		Map<String, List<Map<String, Object>>> parsedJsonSql;
		try {
			parsedJsonSql = objectMapper.readValue(
				StreamUtils.copyToString(
					new ClassPathResource(filePath).getInputStream(),
					Charset.defaultCharset()
				),
				Map.class
			);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to load and parse the file: " + filePath, e);
		}
		for (String query : createInsertQueries(parsedJsonSql)) {
			jdbcTemplate.execute(query);
		}
	}

	private List<String> createInsertQueries(Map<String, List<Map<String, Object>>> parsedJsonSql) {
		return parsedJsonSql.entrySet().stream()
			.flatMap(entry -> entry.getValue().stream().map(row -> {
				String columns = String.join(", ", row.keySet());
				String values = row.values().stream()
					.map(this::formatValue)
					.collect(Collectors.joining(", "));
				return "INSERT INTO " + entry.getKey() + " (" + columns + ") VALUES (" + values + ");";
			}))
			.toList();
	}

	private String formatValue(Object value) {
		if (value == null) {
			return "NULL";
		}
		if (value instanceof String && "now()".equalsIgnoreCase((String)value)) {
			return "now()";
		}
		return "'" + value + "'";
	}

	@Override
	public void afterTestMethod(TestContext testContext) {
		JdbcTemplate jdbcTemplate = testContext.getApplicationContext().getBean(JdbcTemplate.class);
		List<String> truncateQueries = jdbcTemplate.queryForList(
			"""
				SELECT Concat('TRUNCATE TABLE ', TABLE_NAME, ';') AS q
				FROM INFORMATION_SCHEMA.TABLES
				WHERE TABLE_SCHEMA = 'PUBLIC'
				""",
			String.class);
		truncateTables(jdbcTemplate, truncateQueries);
	}

	private void truncateTables(JdbcTemplate jdbcTemplate, List<String> truncateQueries) {
		jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
		truncateQueries.forEach(jdbcTemplate::execute);
		jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
	}
}
