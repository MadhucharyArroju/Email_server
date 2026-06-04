package com.example.email;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Entry point for the Email microservice.
 *
 * Spring Cloud features enabled:
 *   @EnableDiscoveryClient — registers with Eureka service registry
 *   @EnableRetry           — activates @Retryable on email provider beans
 *
 * Insurance Claims & Policy Management System
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableRetry
public class EmailServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmailServiceApplication.class, args);
    }
}
