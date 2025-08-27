package com.smartdrive.userservice.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * GatewayAuthenticationFilter - Extracts user context from API Gateway headers
 * 
 * This filter reads authentication information from headers set by the API
 * Gateway
 * and populates the UserContext for use by business logic. It also validates
 * that requests are coming from the trusted API Gateway.
 * 
 * Security Features:
 * - Validates internal authentication token
 * - Verifies HMAC signature from gateway
 * - Checks request timestamp to prevent replay attacks
 * - Populates thread-local user context
 */
@Component
@Order(1) // Run early in filter chain
@Slf4j
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    // Header names set by API Gateway
    private static final String HEADER_USER_ID = "X-User-ID";
    private static final String HEADER_USERNAME = "X-User-Username";
    private static final String HEADER_EMAIL = "X-User-Email";
    private static final String HEADER_ROLES = "X-User-Roles";
    private static final String HEADER_REQUEST_ID = "X-Request-ID";
    private static final String HEADER_INTERNAL_AUTH = "X-Internal-Auth";
    private static final String HEADER_GATEWAY_SIGNATURE = "X-Gateway-Signature";
    private static final String HEADER_GATEWAY_TIMESTAMP = "X-Gateway-Timestamp";
    private static final String HEADER_FORWARDED_BY = "X-Forwarded-By";

    @Value("${auth.service.internal.key:}")
    private String internalAuthKey;

    @Value("${gateway.signature.secret:}")
    private String signatureSecret;

    @Value("${app.security.gateway-validation.enabled:true}")
    private boolean gatewayValidationEnabled;

    @Value("${app.security.signature-validation.enabled:true}")
    private boolean signatureValidationEnabled;

    // Allow 5 minutes clock skew
    private static final long MAX_TIMESTAMP_SKEW_SECONDS = 300;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        log.debug("🔍 Processing request: {} {}", request.getMethod(), requestPath);

        try {
            // Skip validation for health checks and public endpoints
            if (isPublicEndpoint(requestPath)) {
                log.debug("✅ Public endpoint, skipping authentication: {}", requestPath);
                UserContextHolder.setContext(UserContext.anonymous());
                filterChain.doFilter(request, response);
                return;
            }

            // Validate request comes from trusted gateway
            if (gatewayValidationEnabled && !validateGatewayRequest(request)) {
                log.warn("❌ Invalid gateway request for path: {}", requestPath);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter()
                        .write("{\"error\":\"Unauthorized\",\"message\":\"Request must come through API Gateway\"}");
                return;
            }

            // Extract user context from headers
            UserContext userContext = extractUserContext(request);
            UserContextHolder.setContext(userContext);

            log.debug("✅ User context set: {}", userContext);

            // Continue with request
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("❌ Error processing authentication filter", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter()
                    .write("{\"error\":\"Internal Server Error\",\"message\":\"Authentication processing failed\"}");
        } finally {
            // Always clear context to prevent memory leaks
            UserContextHolder.clear();
        }
    }

    /**
     * Validate that request comes from trusted API Gateway
     */
    private boolean validateGatewayRequest(HttpServletRequest request) {
        // Check internal auth header
        String internalAuth = request.getHeader(HEADER_INTERNAL_AUTH);
        if (internalAuth == null || !internalAuth.equals(internalAuthKey)) {
            log.warn("⚠️ Missing or invalid internal auth header");
            return false;
        }

        // Check forwarded by header
        String forwardedBy = request.getHeader(HEADER_FORWARDED_BY);
        if (!"SmartDrive-Gateway".equals(forwardedBy)) {
            log.warn("⚠️ Missing or invalid forwarded-by header");
            return false;
        }

        // Validate HMAC signature if enabled
        if (signatureValidationEnabled) {
            return validateSignature(request);
        }

        return true;
    }

    /**
     * Validate HMAC signature from gateway
     */
    private boolean validateSignature(HttpServletRequest request) {
        String signature = request.getHeader(HEADER_GATEWAY_SIGNATURE);
        String timestamp = request.getHeader(HEADER_GATEWAY_TIMESTAMP);
        String userId = request.getHeader(HEADER_USER_ID);

        if (signature == null || timestamp == null) {
            log.warn("⚠️ Missing signature or timestamp headers");
            return false;
        }

        try {
            // Check timestamp to prevent replay attacks
            long requestTime = Long.parseLong(timestamp);
            long currentTime = Instant.now().getEpochSecond();
            long timeDiff = Math.abs(currentTime - requestTime);

            if (timeDiff > MAX_TIMESTAMP_SKEW_SECONDS) {
                log.warn("⚠️ Request timestamp too old or in future: {} seconds", timeDiff);
                return false;
            }

            // Generate expected signature
            String expectedSignature = generateSignature(userId, request.getRequestURI(), timestamp);

            // Compare signatures
            if (!signature.equals(expectedSignature)) {
                log.warn("⚠️ Signature validation failed");
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("❌ Error validating signature", e);
            return false;
        }
    }

    /**
     * Generate HMAC signature for validation
     */
    private String generateSignature(String userId, String path, String timestamp) {
        try {
            String data = (userId != null ? userId : "") + "|" + path + "|" + timestamp;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(signatureSecret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) {
            log.error("❌ Failed to generate signature", e);
            return "invalid";
        }
    }

    /**
     * Extract user context from gateway headers
     */
    private UserContext extractUserContext(HttpServletRequest request) {
        String userId = request.getHeader(HEADER_USER_ID);
        String username = request.getHeader(HEADER_USERNAME);
        String email = request.getHeader(HEADER_EMAIL);
        String roles = request.getHeader(HEADER_ROLES);
        String requestId = request.getHeader(HEADER_REQUEST_ID);

        // If no user ID, treat as anonymous
        if (userId == null || userId.trim().isEmpty()) {
            log.debug("🔓 No user ID found, creating anonymous context");
            return UserContext.anonymous();
        }

        // Create authenticated context
        UserContext context = UserContext.authenticated(userId, username, email, roles, requestId);
        log.debug("🔐 Created authenticated context for user: {} with roles: {}", username, roles);

        return context;
    }

    /**
     * Check if endpoint is public (no authentication required)
     */
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/actuator/health") ||
                path.startsWith("/actuator/info") ||
                path.equals("/api/v1/users/register") ||
                path.startsWith("/api/v1/users/verify-email") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs");
    }
}
