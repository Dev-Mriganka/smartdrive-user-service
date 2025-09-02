package com.smartdrive.userservice.service;

// RoleType removed - using String for role types in hybrid approach
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Basic implementation of AuditService for logging admin operations
 * In production, this should write to a secure, immutable audit log system
 */
@Service
@Slf4j
public class AuditServiceImpl implements AuditService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public void logAdminInvitation(String invitedByEmail, String invitedEmail, String role, String clientIp) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        log.info("AUDIT | {} | ADMIN_INVITATION | invitedBy={} | invitedEmail={} | role={} | ip={}", 
                timestamp, invitedByEmail, invitedEmail, role, clientIp);
    }

    @Override
    public void logAdminInvitationAccepted(String adminEmail, String invitedByEmail, String clientIp) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        log.info("AUDIT | {} | ADMIN_INVITATION_ACCEPTED | adminEmail={} | invitedBy={} | ip={}", 
                timestamp, adminEmail, invitedByEmail, clientIp);
    }

    @Override
    public void logAdminInvitationCanceled(String invitedEmail, String canceledByEmail, String clientIp) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        log.info("AUDIT | {} | ADMIN_INVITATION_CANCELED | invitedEmail={} | canceledBy={} | ip={}", 
                timestamp, invitedEmail, canceledByEmail, clientIp);
    }

    @Override
    public void logAdminLogin(String adminEmail, String clientIp, String userAgent, boolean successful) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String result = successful ? "SUCCESS" : "FAILURE";
        log.info("AUDIT | {} | ADMIN_LOGIN_{} | adminEmail={} | ip={} | userAgent={}", 
                timestamp, result, adminEmail, clientIp, userAgent);
    }

    @Override
    public void logAdminLogout(String adminEmail, String clientIp, String sessionId) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        log.info("AUDIT | {} | ADMIN_LOGOUT | adminEmail={} | ip={} | sessionId={}", 
                timestamp, adminEmail, clientIp, sessionId);
    }

    @Override
    public void logAdminPasswordChange(String adminEmail, String clientIp) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        log.info("AUDIT | {} | ADMIN_PASSWORD_CHANGE | adminEmail={} | ip={}", 
                timestamp, adminEmail, clientIp);
    }

    @Override
    public void logAdmin2FASetup(String adminEmail, String clientIp, String method) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        log.info("AUDIT | {} | ADMIN_2FA_SETUP | adminEmail={} | ip={} | method={}", 
                timestamp, adminEmail, clientIp, method);
    }

    @Override
    public void logAdminRoleAssignment(String adminEmail, String targetUserEmail, String role, String clientIp) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        log.info("AUDIT | {} | ADMIN_ROLE_ASSIGNMENT | adminEmail={} | targetUser={} | role={} | ip={}", 
                timestamp, adminEmail, targetUserEmail, role, clientIp);
    }

    @Override
    public void logAdminUserAction(String adminEmail, String targetUserEmail, String action, String clientIp) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        log.info("AUDIT | {} | ADMIN_USER_ACTION | adminEmail={} | targetUser={} | action={} | ip={}", 
                timestamp, adminEmail, targetUserEmail, action, clientIp);
    }

    @Override
    public void logAdminSecurityEvent(String adminEmail, String event, String details, String clientIp) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        log.info("AUDIT | {} | ADMIN_SECURITY_EVENT | adminEmail={} | event={} | details={} | ip={}", 
                timestamp, adminEmail, event, details, clientIp);
    }

    @Override
    public void logUserAction(UUID userId, String action, String description, String details) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        log.info("AUDIT | {} | USER_ACTION | userId={} | action={} | description={} | details={}", 
                timestamp, userId, action, description, details);
    }

    @Override
    public void logSystemAction(String action, String description, String details) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        log.info("AUDIT | {} | SYSTEM_ACTION | action={} | description={} | details={}", 
                timestamp, action, description, details);
    }
}
