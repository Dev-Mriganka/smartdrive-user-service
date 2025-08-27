package com.smartdrive.userservice.security;

import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserContext - Simple POJO to hold authenticated user information
 * 
 * This class represents the authenticated user context extracted from
 * API Gateway headers. It contains all necessary information for
 * business logic decisions without any Spring Security dependencies.
 * 
 * The API Gateway is responsible for authentication and passes user
 * information via headers that this service trusts completely.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {

    /**
     * Unique user identifier from JWT subject
     */
    private String userId;

    /**
     * Username for display and logging
     */
    private String username;

    /**
     * User's email address
     */
    private String email;

    /**
     * User roles for authorization decisions
     */
    private Set<String> roles;

    /**
     * Request correlation ID for tracing
     */
    private String requestId;

    /**
     * Indicates if this is an authenticated request
     */
    private boolean authenticated;

    /**
     * Check if user has a specific role
     * 
     * @param role Role to check
     * @return true if user has the role
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * Check if user has any of the specified roles
     * 
     * @param requiredRoles Roles to check
     * @return true if user has at least one role
     */
    public boolean hasAnyRole(String... requiredRoles) {
        if (roles == null || requiredRoles == null) {
            return false;
        }

        for (String role : requiredRoles) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user has admin privileges
     * 
     * @return true if user is admin
     */
    public boolean isAdmin() {
        return hasRole("SMARTDRIVE_ADMIN");
    }

    /**
     * Check if user is a regular user
     * 
     * @return true if user has user role
     */
    public boolean isUser() {
        return hasRole("SMARTDRIVE_USER");
    }

    /**
     * Check if user can access their own data
     * 
     * @param targetUserId User ID to check access for
     * @return true if user can access the data
     */
    public boolean canAccessUser(String targetUserId) {
        // Admin can access any user
        if (isAdmin()) {
            return true;
        }

        // Users can only access their own data
        return userId != null && userId.equals(targetUserId);
    }

    /**
     * Create anonymous/unauthenticated context
     * 
     * @return UserContext for anonymous user
     */
    public static UserContext anonymous() {
        return UserContext.builder()
                .authenticated(false)
                .roles(new HashSet<>())
                .build();
    }

    /**
     * Create authenticated context from gateway headers
     * 
     * @param userId    User ID
     * @param username  Username
     * @param email     Email
     * @param roles     Comma-separated roles
     * @param requestId Request correlation ID
     * @return UserContext for authenticated user
     */
    public static UserContext authenticated(String userId, String username,
            String email, String roles, String requestId) {
        Set<String> roleSet = new HashSet<>();
        if (roles != null && !roles.trim().isEmpty()) {
            String[] roleArray = roles.split(",");
            for (String role : roleArray) {
                roleSet.add(role.trim());
            }
        }

        return UserContext.builder()
                .userId(userId)
                .username(username)
                .email(email)
                .roles(roleSet)
                .requestId(requestId)
                .authenticated(true)
                .build();
    }

    @Override
    public String toString() {
        return String.format("UserContext{userId='%s', username='%s', authenticated=%s, roles=%s}",
                userId, username, authenticated, roles);
    }
}
