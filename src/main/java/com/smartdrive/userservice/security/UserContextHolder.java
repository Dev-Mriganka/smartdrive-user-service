package com.smartdrive.userservice.security;

import lombok.extern.slf4j.Slf4j;

/**
 * UserContextHolder - Thread-local storage for user context
 * 
 * This class provides thread-safe access to the current user context
 * similar to Spring Security's SecurityContextHolder but much simpler.
 * 
 * The context is populated by a filter that reads headers from the
 * API Gateway and cleared after request processing.
 */
@Slf4j
public class UserContextHolder {

    private static final ThreadLocal<UserContext> contextHolder = new ThreadLocal<>();

    /**
     * Set the user context for the current thread
     * 
     * @param context UserContext to set
     */
    public static void setContext(UserContext context) {
        if (context != null) {
            log.debug("Setting user context for user: {} (requestId: {})",
                    context.getEmail(), context.getRequestId());
        }
        contextHolder.set(context);
    }

    /**
     * Get the current user context
     * 
     * @return Current UserContext or anonymous if none set
     */
    public static UserContext getContext() {
        UserContext context = contextHolder.get();
        if (context == null) {
            log.debug("No user context found, returning anonymous");
            return UserContext.anonymous();
        }
        return context;
    }

    /**
     * Get the current user context, throwing exception if not authenticated
     * 
     * @return Current UserContext
     * @throws IllegalStateException if user is not authenticated
     */
    public static UserContext getAuthenticatedContext() {
        UserContext context = getContext();
        if (!context.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }
        return context;
    }

    /**
     * Check if current user is authenticated
     * 
     * @return true if user is authenticated
     */
    public static boolean isAuthenticated() {
        return getContext().isAuthenticated();
    }

    /**
     * Get current user ID
     * 
     * @return User ID or null if not authenticated
     */
    public static String getCurrentUserId() {
        return getContext().getUserId();
    }

    /**
     * Get current user email
     * 
     * @return Email or null if not authenticated
     */
    public static String getCurrentEmail() {
        return getContext().getEmail();
    }

    /**
     * Check if current user has specific role
     * 
     * @param role Role to check
     * @return true if user has role
     */
    public static boolean hasRole(String role) {
        return getContext().hasRole(role);
    }

    /**
     * Check if current user is admin
     * 
     * @return true if user is admin
     */
    public static boolean isCurrentUserAdmin() {
        return getContext().isAdmin();
    }

    /**
     * Check if current user can access specific user data
     * 
     * @param targetUserId Target user ID
     * @return true if access is allowed
     */
    public static boolean canCurrentUserAccess(String targetUserId) {
        return getContext().canAccessUser(targetUserId);
    }

    /**
     * Clear the user context for the current thread
     * IMPORTANT: Must be called after request processing to prevent memory leaks
     */
    public static void clear() {
        UserContext context = contextHolder.get();
        if (context != null) {
            log.debug("Clearing user context for user: {} (requestId: {})",
                    context.getEmail(), context.getRequestId());
        }
        contextHolder.remove();
    }

    /**
     * Execute a block of code with a specific user context
     * 
     * @param context  UserContext to use
     * @param runnable Code to execute
     */
    public static void executeWithContext(UserContext context, Runnable runnable) {
        UserContext previousContext = contextHolder.get();
        try {
            setContext(context);
            runnable.run();
        } finally {
            if (previousContext != null) {
                setContext(previousContext);
            } else {
                clear();
            }
        }
    }
}
