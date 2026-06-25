package com.myproject.salmon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SalmonApplication {
	public static void main(String[] args) {
		SpringApplication.run(SalmonApplication.class, args);
	}
}
