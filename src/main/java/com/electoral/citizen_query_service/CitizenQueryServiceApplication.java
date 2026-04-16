package com.electoral.citizen_query_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CitizenQueryServiceApplication {

	public static void main(String[] args) {

		System.out.println("DB_URL=" + System.getenv("DB_URL"));
		
		SpringApplication.run(CitizenQueryServiceApplication.class, args);
	}

}
