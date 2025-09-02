package com.smartdrive.userservice.security;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BusinessAuthorization {

    public boolean canViewUserProfile(String targetUserId) {
        UserContext context = UserContextHolder.getAuthenticatedContext();

        if (context.isAdmin()) {
            log.debug("✅ Admin user {} can view profile for user {}",
                    context.getEmail(), targetUserId);
            return true;
        }

        boolean canAccess = context.canAccessUser(targetUserId);
        log.debug("{} User {} {} view profile for user {}",
                canAccess ? "✅" : "❌",
                context.getEmail(),
                canAccess ? "can" : "cannot",
                targetUserId);

        return canAccess;
    }

    public boolean canUpdateUserProfile(String targetUserId) {
        UserContext context = UserContextHolder.getAuthenticatedContext();

        if (context.isAdmin()) {
            log.debug("✅ Admin user {} can update profile for user {}",
                    context.getEmail(), targetUserId);
            return true;
        }

        boolean canAccess = context.canAccessUser(targetUserId);
        log.debug("{} User {} {} update profile for user {}",
                canAccess ? "✅" : "❌",
                context.getEmail(),
                canAccess ? "can" : "cannot",
                targetUserId);

        return canAccess;
    }

    public boolean canDeleteUser(String targetUserId) {
        UserContext context = UserContextHolder.getAuthenticatedContext();

        if (!context.isAdmin()) {
            log.debug("❌ Non-admin user {} cannot delete user {}",
                    context.getEmail(), targetUserId);
            return false;
        }

        if (context.getUserId().equals(targetUserId)) {
            log.debug("❌ Admin user {} cannot delete themselves", context.getEmail());
            return false;
        }

        log.debug("✅ Admin user {} can delete user {}",
                context.getEmail(), targetUserId);
        return true;
    }

    public boolean canAccessAdminFunctions() {
        UserContext context = UserContextHolder.getAuthenticatedContext();

        boolean isAdmin = context.isAdmin();
        log.debug("{} User {} {} access admin functions",
                isAdmin ? "✅" : "❌",
                context.getEmail(),
                isAdmin ? "can" : "cannot");

        return isAdmin;
    }

    public boolean canViewAllUsers() {
        return canAccessAdminFunctions();
    }

    public boolean canCreateUserAccount() {
        return canAccessAdminFunctions();
    }

    public boolean canChangeUserRoles(String targetUserId) {
        UserContext context = UserContextHolder.getAuthenticatedContext();

        if (!context.isAdmin()) {
            log.debug("❌ Non-admin user {} cannot change roles for user {}",
                    context.getEmail(), targetUserId);
            return false;
        }

        if (context.getUserId().equals(targetUserId)) {
            log.debug("❌ Admin user {} cannot change their own roles", context.getEmail());
            return false;
        }

        log.debug("✅ Admin user {} can change roles for user {}",
                context.getEmail(), targetUserId);
        return true;
    }

    public boolean canViewUserActivity(String targetUserId) {
        return canViewUserProfile(targetUserId);
    }

    public void requireAuthenticated() {
        if (!UserContextHolder.isAuthenticated()) {
            throw new UnauthorizedException("Authentication required");
        }
    }

    public void requireAdmin() {
        requireAuthenticated();
        if (!UserContextHolder.isCurrentUserAdmin()) {
            throw new UnauthorizedException("Admin privileges required");
        }
    }

    public void requireCanAccessUser(String targetUserId) {
        requireAuthenticated();
        if (!UserContextHolder.canCurrentUserAccess(targetUserId)) {
            throw new UnauthorizedException("Access denied for user: " + targetUserId);
        }
    }

    // Remove the duplicate method that was at the end!

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }
}