package com.smartdrive.userservice.controller;

import com.smartdrive.userservice.security.BusinessAuthorization;
import com.smartdrive.userservice.security.UserContext;
import com.smartdrive.userservice.security.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Test Controller for Infrastructure-First Security
 * 
 * This controller demonstrates and tests the new security approach
 * without depending on the full User entity and service complexity.
 */
@RestController
@RequestMapping("/api/v1/security-test")
@RequiredArgsConstructor
@Slf4j
public class SecurityTestController {

    private final BusinessAuthorization businessAuth;

    /**
     * Test endpoint - requires authentication
     */
    @GetMapping("/protected")
    public ResponseEntity<Map<String, Object>> protectedEndpoint() {
        log.info("🔍 Testing protected endpoint");
        
        UserContext userContext = UserContextHolder.getAuthenticatedContext();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Access granted to protected endpoint");
        response.put("userId", userContext.getUserId());
        response.put("email", userContext.getEmail());
        response.put("roles", userContext.getRoles());
        response.put("requestId", userContext.getRequestId());
        response.put("authenticated", userContext.isAuthenticated());
        
        log.info("✅ Protected endpoint accessed by user: {}", userContext.getEmail());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Public test endpoint - no authentication required
     */
    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> publicEndpoint() {
        log.info("🔓 Testing public endpoint");
        
        UserContext userContext = UserContextHolder.getContext();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Public endpoint accessible to all");
        response.put("authenticated", userContext.isAuthenticated());
        
        if (userContext.isAuthenticated()) {
            response.put("userId", userContext.getUserId());
            response.put("email", userContext.getEmail());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Admin only endpoint
     */
    @GetMapping("/admin")
    public ResponseEntity<Map<String, Object>> adminEndpoint() {
        log.info("👑 Testing admin endpoint");
        
        UserContext userContext = UserContextHolder.getAuthenticatedContext();
        
        if (!businessAuth.canAccessAdminFunctions()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Forbidden");
            error.put("message", "Admin privileges required");
            return ResponseEntity.status(403).body(error);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Admin endpoint accessed successfully");
        response.put("userId", userContext.getUserId());
        response.put("email", userContext.getEmail());
        response.put("isAdmin", userContext.isAdmin());
        response.put("requestId", userContext.getRequestId());
        
        log.info("✅ Admin endpoint accessed by user: {}", userContext.getEmail());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Test user-specific resource access
     */
    @GetMapping("/user/{userId}/data")
    public ResponseEntity<Map<String, Object>> userDataEndpoint(@PathVariable String userId) {
        log.info("👤 Testing user-specific endpoint for userId: {}", userId);
        
        UserContext userContext = UserContextHolder.getAuthenticatedContext();
        
        if (!businessAuth.canViewUserProfile(userId)) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Forbidden");
            error.put("message", "Cannot access this user's data");
            error.put("targetUserId", userId);
            error.put("currentUserId", userContext.getUserId());
            return ResponseEntity.status(403).body(error);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User data access granted");
        response.put("targetUserId", userId);
        response.put("accessedBy", userContext.getUserId());
        response.put("accessorEmail", userContext.getEmail());
        response.put("requestId", userContext.getRequestId());
        
        log.info("✅ User data accessed: {} by {}", userId, userContext.getEmail());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Test current user context
     */
    @GetMapping("/whoami")
    public ResponseEntity<Map<String, Object>> whoAmI() {
        log.info("🤔 Testing user context extraction");
        
        UserContext userContext = UserContextHolder.getContext();
        
        Map<String, Object> response = new HashMap<>();
        
        if (userContext.isAuthenticated()) {
            response.put("authenticated", true);
            response.put("userId", userContext.getUserId());
            response.put("email", userContext.getEmail());
            response.put("roles", userContext.getRoles());
            response.put("requestId", userContext.getRequestId());
            response.put("isAdmin", userContext.isAdmin());
            response.put("isUser", userContext.isUser());
        } else {
            response.put("authenticated", false);
            response.put("message", "Anonymous user");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Test role checking
     */
    @GetMapping("/roles/check")
    public ResponseEntity<Map<String, Object>> checkRoles(@RequestParam(required = false) String role) {
        log.info("🎭 Testing role checking for role: {}", role);
        
        UserContext userContext = UserContextHolder.getAuthenticatedContext();
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userContext.getUserId());
        response.put("email", userContext.getEmail());
        response.put("allRoles", userContext.getRoles());
        
        if (role != null) {
            boolean hasRole = userContext.hasRole(role);
            response.put("checkedRole", role);
            response.put("hasRole", hasRole);
        }
        
        response.put("isAdmin", userContext.isAdmin());
        response.put("isUser", userContext.isUser());
        
        return ResponseEntity.ok(response);
    }
}
