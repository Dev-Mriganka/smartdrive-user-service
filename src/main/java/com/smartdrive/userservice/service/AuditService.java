package com.smartdrive.userservice.service;

import java.util.UUID;

/**
 * Service interface for audit logging of admin operations
 * Implementations should ensure secure, immutable audit trails
 */
public interface AuditService {

    /**
     * Log admin invitation creation
     */
    void logAdminInvitation(String invitedByEmail, String invitedEmail, String role, String clientIp);

    /**
     * Log admin invitation acceptance
     */
    void logAdminInvitationAccepted(String adminEmail, String invitedByEmail, String clientIp);

    /**
     * Log admin invitation cancellation
     */
    void logAdminInvitationCanceled(String invitedEmail, String canceledByEmail, String clientIp);

    /**
     * Log admin login attempt
     */
    void logAdminLogin(String adminEmail, String clientIp, String userAgent, boolean successful);

    /**
     * Log admin logout
     */
    void logAdminLogout(String adminEmail, String clientIp, String sessionId);

    /**
     * Log admin password change
     */
    void logAdminPasswordChange(String adminEmail, String clientIp);

    /**
     * Log admin 2FA setup
     */
    void logAdmin2FASetup(String adminEmail, String clientIp, String method);

    /**
     * Log admin role assignment
     */
    void logAdminRoleAssignment(String adminEmail, String targetUserEmail, String role, String clientIp);

    /**
     * Log admin user management action
     */
    void logAdminUserAction(String adminEmail, String targetUserEmail, String action, String clientIp);

    /**
     * Log admin security event
     */
    void logAdminSecurityEvent(String adminEmail, String event, String details, String clientIp);

    /**
     * Log user action (for event-driven operations)
     */
    void logUserAction(UUID userId, String action, String description, String details);

    /**
     * Log system action (for automated operations)
     */
    void logSystemAction(String action, String description, String details);
}
