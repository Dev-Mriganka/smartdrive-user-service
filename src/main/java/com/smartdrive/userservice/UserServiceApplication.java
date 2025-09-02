package com.smartdrive.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * User Service Application
 * 
 * Handles all user management operations including:
 * - User registration and profile management
 * - Password management and 2FA
 * - Email verification
 * - User preferences and settings
 * 
 * SECURITY NOTE: No Spring Security dependency - API Gateway handles all security.
 * This service trusts requests coming through the gateway and validates internal headers.
 */
@SpringBootApplication(exclude = {FlywayAutoConfiguration.class})  // Exclude Flyway auto-configuration
@EnableCaching
@EnableScheduling
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}