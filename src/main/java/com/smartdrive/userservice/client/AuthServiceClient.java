package com.smartdrive.userservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * Client for communicating with Auth Service
 * Used for consistency checks and fallback operations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthServiceClient {

    private final RestTemplate restTemplate;

    @Value("${auth.service.url:http://auth-service:8082}")
    private String authServiceUrl;

    /**
     * Get user email from Auth Service
     */
    public String getUserEmail(UUID authUserId) {
        try {
            String url = authServiceUrl + "/api/v1/auth/users/" + authUserId + "/email";
            String email = restTemplate.getForObject(url, String.class);
            
            if (email == null) {
                throw new RuntimeException("Email not found for user: " + authUserId);
            }
            
            return email;
        } catch (Exception e) {
            log.error("Failed to get email for user: {}", authUserId, e);
            throw new RuntimeException("Failed to get email from Auth Service", e);
        }
    }

    /**
     * Get user email verification status from Auth Service
     */
    public Boolean getUserEmailVerified(UUID authUserId) {
        try {
            String url = authServiceUrl + "/api/v1/auth/users/" + authUserId + "/email-verified";
            Boolean verified = restTemplate.getForObject(url, Boolean.class);
            
            if (verified == null) {
                throw new RuntimeException("Email verification status not found for user: " + authUserId);
            }
            
            return verified;
        } catch (Exception e) {
            log.error("Failed to get email verification status for user: {}", authUserId, e);
            throw new RuntimeException("Failed to get email verification status from Auth Service", e);
        }
    }

    /**
     * Check if user exists in Auth Service
     */
    public Boolean userExists(UUID authUserId) {
        try {
            String url = authServiceUrl + "/api/v1/auth/users/" + authUserId + "/exists";
            Boolean exists = restTemplate.getForObject(url, Boolean.class);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("Failed to check if user exists: {}", authUserId, e);
            return false;
        }
    }
}
