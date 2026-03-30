package com.kodesalon.kopang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class KopangApplication {

	public static void main(String[] args) {
		SpringApplication.run(KopangApplication.class, args);
	}
}
