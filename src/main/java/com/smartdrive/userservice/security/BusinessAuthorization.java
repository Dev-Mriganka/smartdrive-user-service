package com.smartdrive.userservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * BusinessAuthorization - Business logic authorization rules
 * 
 * This class contains all business-level authorization logic for the User Service.
 * Unlike infrastructure security (handled by API Gateway), this focuses on
 * business rules like "users can only modify their own data" or "admins can
 * access all user data".
 * 
 * These rules are independent of the authentication mechanism and can be
 * easily unit tested without security infrastructure.
 */
@Component
@Slf4j
public class BusinessAuthorization {
    
    /**
     * Check if current user can view a specific user's profile
     * 
     * Business Rule: Users can view their own profile, admins can view any profile
     * 
     * @param targetUserId User ID to check access for
     * @return true if access is allowed
     * @throws UnauthorizedException if user is not authenticated
     */
    public boolean canViewUserProfile(String targetUserId) {
        UserContext context = UserContextHolder.getAuthenticatedContext();
        
        // Admin can view any profile
        if (context.isAdmin()) {
            log.debug("✅ Admin user {} can view profile for user {}", 
                     context.getUsername(), targetUserId);
            return true;
        }
        
        // Users can view their own profile
        boolean canAccess = context.canAccessUser(targetUserId);
        log.debug("{} User {} {} view profile for user {}", 
                 canAccess ? "✅" : "❌", 
                 context.getUsername(), 
                 canAccess ? "can" : "cannot", 
                 targetUserId);
        
        return canAccess;
    }
    
    /**
     * Check if current user can update a specific user's profile
     * 
     * Business Rule: Users can update their own profile, admins can update any profile
     * 
     * @param targetUserId User ID to check access for
     * @return true if access is allowed
     */
    public boolean canUpdateUserProfile(String targetUserId) {
        UserContext context = UserContextHolder.getAuthenticatedContext();
        
        // Admin can update any profile
        if (context.isAdmin()) {
            log.debug("✅ Admin user {} can update profile for user {}", 
                     context.getUsername(), targetUserId);
            return true;
        }
        
        // Users can update their own profile
        boolean canAccess = context.canAccessUser(targetUserId);
        log.debug("{} User {} {} update profile for user {}", 
                 canAccess ? "✅" : "❌", 
                 context.getUsername(), 
                 canAccess ? "can" : "cannot", 
                 targetUserId);
        
        return canAccess;
    }
    
    /**
     * Check if current user can delete a specific user
     * 
     * Business Rule: Only admins can delete users, users cannot delete themselves
     * 
     * @param targetUserId User ID to check access for
     * @return true if access is allowed
     */
    public boolean canDeleteUser(String targetUserId) {
        UserContext context = UserContextHolder.getAuthenticatedContext();
        
        // Only admins can delete users
        if (!context.isAdmin()) {
            log.debug("❌ Non-admin user {} cannot delete user {}", 
                     context.getUsername(), targetUserId);
            return false;
        }
        
        // Admin cannot delete themselves (business rule)
        if (context.getUserId().equals(targetUserId)) {
            log.debug("❌ Admin user {} cannot delete themselves", context.getUsername());
            return false;
        }
        
        log.debug("✅ Admin user {} can delete user {}", 
                 context.getUsername(), targetUserId);
        return true;
    }
    
    /**
     * Check if current user can access admin functions
     * 
     * Business Rule: Only users with SMARTDRIVE_ADMIN role can access admin functions
     * 
     * @return true if access is allowed
     */
    public boolean canAccessAdminFunctions() {
        UserContext context = UserContextHolder.getAuthenticatedContext();
        
        boolean isAdmin = context.isAdmin();
        log.debug("{} User {} {} access admin functions", 
                 isAdmin ? "✅" : "❌", 
                 context.getUsername(), 
                 isAdmin ? "can" : "cannot");
        
        return isAdmin;
    }
    
    /**
     * Check if current user can view all users
     * 
     * Business Rule: Only admins can view all users list
     * 
     * @return true if access is allowed
     */
    public boolean canViewAllUsers() {
        return canAccessAdminFunctions();
    }
    
    /**
     * Check if current user can create new user accounts
     * 
     * Business Rule: Only admins can create user accounts (registration is public)
     * 
     * @return true if access is allowed
     */
    public boolean canCreateUserAccount() {
        return canAccessAdminFunctions();
    }
    
    /**
     * Check if current user can change user roles
     * 
     * Business Rule: Only admins can modify user roles
     * 
     * @param targetUserId User ID to check access for
     * @return true if access is allowed
     */
    public boolean canChangeUserRoles(String targetUserId) {
        UserContext context = UserContextHolder.getAuthenticatedContext();
        
        // Only admins can change roles
        if (!context.isAdmin()) {
            log.debug("❌ Non-admin user {} cannot change roles for user {}", 
                     context.getUsername(), targetUserId);
            return false;
        }
        
        // Admin cannot change their own roles (business rule for safety)
        if (context.getUserId().equals(targetUserId)) {
            log.debug("❌ Admin user {} cannot change their own roles", context.getUsername());
            return false;
        }
        
        log.debug("✅ Admin user {} can change roles for user {}", 
                 context.getUsername(), targetUserId);
        return true;
    }
    
    /**
     * Check if current user can access user activity logs
     * 
     * Business Rule: Users can see their own activity, admins can see all activity
     * 
     * @param targetUserId User ID to check access for
     * @return true if access is allowed
     */
    public boolean canViewUserActivity(String targetUserId) {
        return canViewUserProfile(targetUserId); // Same rules as profile viewing
    }
    
    /**
     * Require authenticated user or throw exception
     * 
     * @throws UnauthorizedException if user is not authenticated
     */
    public void requireAuthenticated() {
        if (!UserContextHolder.isAuthenticated()) {
            throw new UnauthorizedException("Authentication required");
        }
    }
    
    /**
     * Require admin user or throw exception
     * 
     * @throws UnauthorizedException if user is not admin
     */
    public void requireAdmin() {
        requireAuthenticated();
        if (!UserContextHolder.isCurrentUserAdmin()) {
            throw new UnauthorizedException("Admin privileges required");
        }
    }
    
    /**
     * Require user can access target user or throw exception
     * 
     * @param targetUserId Target user ID
     * @throws UnauthorizedException if access is denied
     */
    public void requireCanAccessUser(String targetUserId) {
        requireAuthenticated();
        if (!UserContextHolder.canCurrentUserAccess(targetUserId)) {
            throw new UnauthorizedException("Access denied for user: " + targetUserId);
        }
    }
    
    /**
     * Checks if the current authenticated user can access admin functions.
     * @return true if the user is an admin, false otherwise.
     */
    public boolean canAccessAdminFunctions() {
        UserContext userContext = UserContextHolder.getAuthenticatedContext();
        return userContext.isAdmin();
    }

    /**
     * Custom exception for authorization failures
     */
    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }
}
