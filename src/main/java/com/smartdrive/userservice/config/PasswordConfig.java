package com.smartdrive.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password encoder configuration for User Service
 * 
 * NOTE: This provides only the PasswordEncoder bean without enabling Spring Security.
 * API Gateway handles all security - this service only needs password hashing.
 */
@Configuration
public class PasswordConfig {
    
    /**
     * BCrypt password encoder for secure password hashing
     * Uses strength 12 for production-grade security
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
