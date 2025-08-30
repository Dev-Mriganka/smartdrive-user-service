package com.smartdrive.userservice.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartdrive.userservice.dto.CredentialVerificationRequest;
import com.smartdrive.userservice.dto.TokenClaimsResponse;
import com.smartdrive.userservice.dto.UserRegistrationRequest;
import com.smartdrive.userservice.service.AuthService;
import com.smartdrive.userservice.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for authentication-related operations
 * Handles internal requests from the Authorization Server
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    /**
     * Register a new user (public endpoint)
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerUser(
            @Valid @RequestBody UserRegistrationRequest registrationRequest,
            HttpServletRequest request) {
        log.info("🚀 User registration request received for email: {}", registrationRequest.getEmail());
        try {
            String clientIp = getClientIpAddress(request);
            userService.registerUser(registrationRequest, clientIp);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Registration successful. Please check your email to verify your account.");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("❌ Error during user registration for email: {}", registrationRequest.getEmail(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "registration_failed");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Verify user email (public endpoint)
     */
    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam("token") String token) {
        log.info("✉️ Email verification request received for token: {}", token);
        try {
            authService.verifyEmail(token);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Email verified successfully.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error during email verification for token: {}", token, e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "verification_failed");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Verify user credentials (internal endpoint for Authorization Server)
     */
    @PostMapping("/verify-credentials")
    public ResponseEntity<Map<String, Object>> verifyCredentials(
            @Valid @RequestBody CredentialVerificationRequest request,
            HttpServletRequest httpRequest) {

        log.info("🔐 Credential verification request for user: {}", request.getUsername());

        // Verify internal authentication
        String internalAuthHeader = httpRequest.getHeader("X-Internal-Auth");
        if (!authService.validateInternalAuth(internalAuthHeader)) {
            log.warn("❌ Unauthorized internal request from: {}", getClientIpAddress(httpRequest));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "unauthorized",
                    "message", "Invalid internal authentication"));
        }

        try {
            boolean isValid = authService.verifyCredentials(request.getUsername(), request.getPassword());

            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);
            response.put("username", request.getUsername());

            if (isValid) {
                log.info("✅ Credential verification successful for user: {}", request.getUsername());
            } else {
                log.warn("❌ Credential verification failed for user: {}", request.getUsername());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error during credential verification for user: {}", request.getUsername(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "verification_failed");
            errorResponse.put("message", "An error occurred during credential verification");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get user token claims for JWT generation (internal endpoint for Authorization
     * Server)
     */
    @GetMapping("/{username}/token-claims")
    public ResponseEntity<Map<String, Object>> getTokenClaims(
            @PathVariable String username,
            HttpServletRequest httpRequest) {

        log.info("👤 Token claims request for user: {}", username);

        // Verify internal authentication
        String internalAuthHeader = httpRequest.getHeader("X-Internal-Auth");
        if (!authService.validateInternalAuth(internalAuthHeader)) {
            log.warn("❌ Unauthorized internal request from: {}", getClientIpAddress(httpRequest));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "unauthorized",
                    "message", "Invalid internal authentication"));
        }

        try {
            TokenClaimsResponse claims = authService.getTokenClaims(username);

            Map<String, Object> response = new HashMap<>();
            response.put("user_id", claims.getUserId());
            response.put("username", claims.getUsername());
            response.put("email", claims.getEmail());
            response.put("first_name", claims.getFirstName());
            response.put("last_name", claims.getLastName());
            response.put("roles", claims.getRoles());
            response.put("is_enabled", claims.getIsEnabled());
            response.put("is_email_verified", claims.getIsEmailVerified());

            log.info("✅ Token claims retrieved successfully for user: {}", username);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error retrieving token claims for user: {}", username, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "claims_retrieval_failed");
            errorResponse.put("message", "Failed to retrieve user token claims");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get user token claims by user ID for JWT generation (internal endpoint for Authorization Server)
     */
    @GetMapping("/id/{userId}/token-claims")
    public ResponseEntity<Map<String, Object>> getTokenClaimsById(
            @PathVariable String userId,
            HttpServletRequest httpRequest) {

        log.info("👤 Token claims request for user ID: {}", userId);

        // Verify internal authentication
        String internalAuthHeader = httpRequest.getHeader("X-Internal-Auth");
        if (!authService.validateInternalAuth(internalAuthHeader)) {
            log.warn("❌ Unauthorized internal request from: {}", getClientIpAddress(httpRequest));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "unauthorized",
                    "message", "Invalid internal authentication"));
        }

        try {
            TokenClaimsResponse claims = authService.getTokenClaimsById(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("user_id", claims.getUserId());
            response.put("username", claims.getUsername());
            response.put("email", claims.getEmail());
            response.put("first_name", claims.getFirstName());
            response.put("last_name", claims.getLastName());
            response.put("roles", claims.getRoles());
            response.put("is_enabled", claims.getIsEnabled());
            response.put("is_email_verified", claims.getIsEmailVerified());

            log.info("✅ Token claims retrieved successfully for user ID: {}", userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error retrieving token claims for user ID: {}", userId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "claims_retrieval_failed");
            errorResponse.put("message", "Failed to retrieve user token claims by ID");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get user profile for userinfo endpoint (internal endpoint for Authorization
     * Server)
     */
    @GetMapping("/{username}/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(
            @PathVariable String username,
            HttpServletRequest httpRequest) {

        log.info("👤 User profile request for user: {}", username);

        // Verify internal authentication
        String internalAuthHeader = httpRequest.getHeader("X-Internal-Auth");
        if (!authService.validateInternalAuth(internalAuthHeader)) {
            log.warn("❌ Unauthorized internal request from: {}", getClientIpAddress(httpRequest));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "unauthorized",
                    "message", "Invalid internal authentication"));
        }

        try {
            Map<String, Object> profile = authService.getUserProfile(username);

            log.info("✅ User profile retrieved successfully for user: {}", username);
            return ResponseEntity.ok(profile);

        } catch (Exception e) {
            log.error("❌ Error retrieving user profile for user: {}", username, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "profile_retrieval_failed");
            errorResponse.put("message", "Failed to retrieve user profile");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * TEMPORARY: Create admin user for testing
     * TODO: Remove this endpoint in production
     */
    @PostMapping("/create-admin")
    public ResponseEntity<Map<String, Object>> createAdminUser() {
        log.info("🚀 Creating admin user for testing...");
        
        try {
            UserRegistrationRequest adminRequest = new UserRegistrationRequest();
            adminRequest.setUsername("admin");
            adminRequest.setEmail("admin@smartdrive.com");
            adminRequest.setPassword("Admin123!");
            adminRequest.setConfirmPassword("Admin123!");
            adminRequest.setFirstName("System");
            adminRequest.setLastName("Administrator");
            adminRequest.setBio("Default system administrator account");
            
            // Create user with admin role using direct service calls
            authService.createAdminUser(adminRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Admin user created successfully");
            response.put("username", "admin");
            response.put("email", "admin@smartdrive.com");
            response.put("warning", "Default password is 'Admin123!' - Change this immediately!");
            
            log.info("✅ Admin user created successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error creating admin user", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "admin_creation_failed");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0];
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
