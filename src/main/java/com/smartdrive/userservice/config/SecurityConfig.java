package com.smartdrive.userservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${app.security.gateway-validation.enabled:true}")
    private boolean gatewayValidationEnabled;

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

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("🔧 Configuring BCrypt password encoder");
        return new BCryptPasswordEncoder(12);
    }

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