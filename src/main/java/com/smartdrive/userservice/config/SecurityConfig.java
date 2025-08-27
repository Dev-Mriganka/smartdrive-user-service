package com.smartdrive.userservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Simplified Security Configuration for User Service
 * 
 * This configuration replaces traditional Spring Security with a simpler
 * approach that trusts the API Gateway for authentication and authorization.
 * 
 * Key Changes:
 * - Removed OAuth2 Resource Server configuration
 * - Removed SecurityFilterChain (using custom filter instead)
 * - Kept only password encoder for user registration
 * - Gateway handles all security concerns
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    @Value("${app.security.gateway-validation.enabled:true}")
    private boolean gatewayValidationEnabled;

    /**
     * Initialize security configuration
     */
    @Bean
    public SecurityConfiguration securityConfiguration() {
        SecurityConfiguration config = new SecurityConfiguration();
        config.setGatewayValidationEnabled(gatewayValidationEnabled);

        if (gatewayValidationEnabled) {
            log.info("🔐 Gateway validation enabled - User Service trusts API Gateway for authentication");
        } else {
            log.warn("⚠️ Gateway validation disabled - This should only be used for testing!");
        }

        log.info("✅ Simplified security configuration initialized");
        return config;
    }

    /**
     * Configure password encoder for user registration
     * This is still needed for hashing passwords during registration
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("🔧 Configuring BCrypt password encoder");
        return new BCryptPasswordEncoder(12); // Strong hashing rounds
    }

    /**
     * Simple configuration holder
     */
    public static class SecurityConfiguration {
        private boolean gatewayValidationEnabled = true;

        public boolean isGatewayValidationEnabled() {
            return gatewayValidationEnabled;
        }

        public void setGatewayValidationEnabled(boolean gatewayValidationEnabled) {
            this.gatewayValidationEnabled = gatewayValidationEnabled;
        }
    }
}
