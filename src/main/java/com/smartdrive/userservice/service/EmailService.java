package com.smartdrive.userservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for sending emails
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.user.verification.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Send email verification email
     */
    public void sendVerificationEmail(String toEmail, String username, String verificationToken) {
        log.info("📧 Sending verification email to: {}", toEmail);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to SmartDrive - Verify Your Email");

            String verificationUrl = baseUrl + "/verify-email?token=" + verificationToken;

            message.setText(String.format(
                    "Hello %s,\n\n" +
                            "Welcome to SmartDrive! Please verify your email address by clicking the link below:\n\n" +
                            "%s\n\n" +
                            "This link will expire in 24 hours.\n\n" +
                            "If you didn't create an account, please ignore this email.\n\n" +
                            "Best regards,\n" +
                            "The SmartDrive Team",
                    username, verificationUrl));

            mailSender.send(message);
            log.info("✅ Verification email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("❌ Failed to send verification email to: {}", toEmail, e);
            // For testing purposes, don't throw exception - just log the error
            log.warn("⚠️ Email sending failed, but continuing with registration for testing");
        }
    }

    /**
     * Send password change notification
     */
    public void sendPasswordChangeNotification(String toEmail, String username) {
        log.info("📧 Sending password change notification to: {}", toEmail);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("SmartDrive - Password Changed");

            message.setText(String.format(
                    "Hello %s,\n\n" +
                            "Your SmartDrive password has been changed successfully.\n\n" +
                            "If you didn't make this change, please contact support immediately.\n\n" +
                            "Best regards,\n" +
                            "The SmartDrive Team",
                    username));

            mailSender.send(message);
            log.info("✅ Password change notification sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("❌ Failed to send password change notification to: {}", toEmail, e);
            // Don't throw exception for notifications - they're not critical
        }
    }

    /**
     * Send welcome email
     */
    public void sendWelcomeEmail(String toEmail, String username) {
        log.info("📧 Sending welcome email to: {}", toEmail);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to SmartDrive!");

            message.setText(String.format(
                    "Hello %s,\n\n" +
                            "Welcome to SmartDrive! Your account has been successfully created and verified.\n\n" +
                            "You can now start using all the features of SmartDrive.\n\n" +
                            "Best regards,\n" +
                            "The SmartDrive Team",
                    username));

            mailSender.send(message);
            log.info("✅ Welcome email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("❌ Failed to send welcome email to: {}", toEmail, e);
            // Don't throw exception for welcome emails - they're not critical
        }
    }

    /**
     * Send account lock notification
     */
    public void sendAccountLockNotification(String toEmail, String username, String reason) {
        log.info("📧 Sending account lock notification to: {}", toEmail);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("SmartDrive - Account Locked");

            message.setText(String.format(
                    "Hello %s,\n\n" +
                            "Your SmartDrive account has been temporarily locked for security reasons.\n\n" +
                            "Reason: %s\n\n" +
                            "Your account will be automatically unlocked after 30 minutes.\n\n" +
                            "If you believe this is an error, please contact support.\n\n" +
                            "Best regards,\n" +
                            "The SmartDrive Team",
                    username, reason));

            mailSender.send(message);
            log.info("✅ Account lock notification sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("❌ Failed to send account lock notification to: {}", toEmail, e);
            // Don't throw exception for notifications
        }
    }
}
