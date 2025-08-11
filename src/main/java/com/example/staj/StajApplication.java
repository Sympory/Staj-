package com.example.staj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.example.staj.repository")
@EntityScan(basePackages = "com.example.staj")
public class StajApplication {
    public static void main(String[] args) {
        SpringApplication.run(StajApplication.class, args);
    }
}
