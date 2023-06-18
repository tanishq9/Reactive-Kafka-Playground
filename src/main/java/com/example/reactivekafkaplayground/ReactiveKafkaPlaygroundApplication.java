package com.example.reactivekafkaplayground;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.reactivekafkaplayground.sec13.${app}")
public class ReactiveKafkaPlaygroundApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReactiveKafkaPlaygroundApplication.class, args);
	}
}
