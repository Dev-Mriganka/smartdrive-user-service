package com.smartdrive.userservice.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartdrive.userservice.dto.events.EmailChangedEvent;
import com.smartdrive.userservice.dto.events.EmailVerifiedEvent;
import com.smartdrive.userservice.dto.events.UserRegisteredEvent;
import com.smartdrive.userservice.model.UserProfile;
import com.smartdrive.userservice.repository.UserProfileRepository;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event consumer for user-related events from Auth Service
 * Handles email synchronization and user profile creation
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserEventConsumer {

    private final UserProfileRepository userProfileRepository;
    private final AuditService auditService;

    /**
     * Handle user registration event from Auth Service
     * Creates user profile with duplicated email for performance
     */
    @SqsListener("${aws.sqs.user-registered-queue:smartdrive-user-registered-queue}")
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("📧 Received UserRegisteredEvent for user: {}", event.getUserId());

        try {
            // Check if profile already exists
            Optional<UserProfile> existingProfile = userProfileRepository.findByAuthUserId(event.getUserId());
            if (existingProfile.isPresent()) {
                log.warn("⚠️ User profile already exists for auth_user_id: {}", event.getUserId());
                return;
            }

            // Create new user profile with duplicated email
            UserProfile profile = UserProfile.builder()
                    .authUserId(event.getUserId())
                    .email(event.getEmail())
                    .emailVerified(event.getEmailVerified())
                    .firstName(event.getFirstName())
                    .lastName(event.getLastName())
                    .displayName(event.getFirstName() + " " + event.getLastName())
                    .build();

            UserProfile savedProfile = userProfileRepository.save(profile);

            // Audit the profile creation
            auditService.logUserAction(
                    savedProfile.getId(),
                    "USER_PROFILE_CREATED",
                    "User profile created from Auth Service event",
                    null);

            log.info("✅ User profile created successfully for user: {} with email: {}",
                    event.getUserId(), event.getEmail());

        } catch (Exception e) {
            log.error("❌ Failed to handle UserRegisteredEvent for user: {}", event.getUserId(), e);
            throw e; // Re-throw to trigger retry mechanism
        }
    }

    /**
     * Handle email verification event from Auth Service
     * Updates email verification status in User Service
     */
    @SqsListener("${aws.sqs.email-verified-queue:smartdrive-email-verified-queue}")
    public void handleEmailVerified(EmailVerifiedEvent event) {
        log.info("✅ Received EmailVerifiedEvent for user: {} with email: {}",
                event.getUserId(), event.getEmail());

        try {
            UserProfile profile = userProfileRepository.findByAuthUserId(event.getUserId())
                    .orElseThrow(() -> new RuntimeException(
                            "User profile not found for auth_user_id: " + event.getUserId()));

            // Update email verification status
            profile.markEmailVerified();
            userProfileRepository.save(profile);

            // Audit the email verification
            auditService.logUserAction(
                    profile.getId(),
                    "EMAIL_VERIFIED",
                    "Email verified via Auth Service event",
                    null);

            log.info("✅ Email verification status updated for user: {}", event.getUserId());

        } catch (Exception e) {
            log.error("❌ Failed to handle EmailVerifiedEvent for user: {}", event.getUserId(), e);
            throw e;
        }
    }

    /**
     * Handle email change event from Auth Service
     * Updates email in User Service and marks as unverified
     */
    @SqsListener("${aws.sqs.email-changed-queue:smartdrive-email-changed-queue}")
    public void handleEmailChanged(EmailChangedEvent event) {
        log.info("📧 Received EmailChangedEvent for user: {} from {} to {}",
                event.getUserId(), event.getOldEmail(), event.getNewEmail());

        try {
            UserProfile profile = userProfileRepository.findByAuthUserId(event.getUserId())
                    .orElseThrow(() -> new RuntimeException(
                            "User profile not found for auth_user_id: " + event.getUserId()));

            // Update email and verification status
            profile.updateEmail(event.getNewEmail());
            profile.setEmailVerified(event.getEmailVerified());
            userProfileRepository.save(profile);

            // Audit the email change
            auditService.logUserAction(
                    profile.getId(),
                    "EMAIL_CHANGED",
                    "Email changed from " + event.getOldEmail() + " to " + event.getNewEmail(),
                    null);

            log.info("✅ Email updated for user: {} from {} to {}",
                    event.getUserId(), event.getOldEmail(), event.getNewEmail());

        } catch (Exception e) {
            log.error("❌ Failed to handle EmailChangedEvent for user: {}", event.getUserId(), e);
            throw e;
        }
    }
}
