package com.smartdrive.userservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdrive.userservice.event.UserEmailChangedEvent;
import com.smartdrive.userservice.event.UserRegisteredEvent;
import com.smartdrive.userservice.event.UserVerifiedEvent;
import com.smartdrive.userservice.model.UserProfile;
import com.smartdrive.userservice.repository.UserProfileRepository;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Handles user-related events from the message queue
 * Implements the async processing flow from the sequence diagram
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventHandler {

    private final UserProfileRepository userProfileRepository;
    private final ObjectMapper objectMapper;

    /**
     * Handle UserRegisteredEvent from Auth Service
     * Creates user profile when user registers
     */
    @SqsListener("${aws.sqs.user-registered-queue}")
    @Transactional
    public void handleUserRegistered(String message) {
        try {
            log.info("📨 Received UserRegisteredEvent: {}", message);
            
            UserRegisteredEvent event = objectMapper.readValue(message, UserRegisteredEvent.class);
            
            // Check if user profile already exists
            Optional<UserProfile> existingProfile = userProfileRepository.findByAuthUserId(event.getAuthUserId());
            if (existingProfile.isPresent()) {
                log.warn("⚠️ User profile already exists for auth user ID: {}", event.getAuthUserId());
                return;
            }
            
            // Create user profile with duplicated email for performance
            UserProfile userProfile = UserProfile.builder()
                    .authUserId(event.getAuthUserId())
                    .email(event.getEmail().toLowerCase().trim()) // DUPLICATED from auth service
                    .firstName(event.getFirstName())
                    .lastName(event.getLastName())
                    .emailVerified(event.isEmailVerified())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            UserProfile savedProfile = userProfileRepository.save(userProfile);
            log.info("✅ User profile created for auth user ID: {} with email: {}", 
                    savedProfile.getAuthUserId(), savedProfile.getEmail());
            
            // TODO: Publish UserProfileCreatedEvent if needed
            
        } catch (Exception e) {
            log.error("❌ Error handling UserRegisteredEvent: {}", message, e);
            throw new RuntimeException("Failed to handle UserRegisteredEvent", e);
        }
    }

    /**
     * Handle UserVerifiedEvent from Auth Service
     * Updates email verification status in user profile
     */
    @SqsListener("${aws.sqs.email-verified-queue}")
    @Transactional
    @CacheEvict(value = "userProfile", key = "#event.authUserId")
    public void handleUserVerified(String message) {
        try {
            log.info("📨 Received UserVerifiedEvent: {}", message);
            
            UserVerifiedEvent event = objectMapper.readValue(message, UserVerifiedEvent.class);
            
            Optional<UserProfile> profileOpt = userProfileRepository.findByAuthUserId(event.getAuthUserId());
            if (profileOpt.isEmpty()) {
                log.warn("⚠️ User profile not found for auth user ID: {}", event.getAuthUserId());
                return;
            }
            
            UserProfile profile = profileOpt.get();
            profile.markEmailVerified();
            profile.setUpdatedAt(LocalDateTime.now());
            
            userProfileRepository.save(profile);
            log.info("✅ User profile email verified for auth user ID: {}", event.getAuthUserId());
            
        } catch (Exception e) {
            log.error("❌ Error handling UserVerifiedEvent: {}", message, e);
            throw new RuntimeException("Failed to handle UserVerifiedEvent", e);
        }
    }

    /**
     * Handle UserEmailChangedEvent from Auth Service
     * Syncs email changes across services
     */
    @SqsListener("${aws.sqs.email-changed-queue}")
    @Transactional
    @CacheEvict(value = "userProfile", key = "#event.authUserId")
    public void handleUserEmailChanged(String message) {
        try {
            log.info("📨 Received UserEmailChangedEvent: {}", message);
            
            UserEmailChangedEvent event = objectMapper.readValue(message, UserEmailChangedEvent.class);
            
            Optional<UserProfile> profileOpt = userProfileRepository.findByAuthUserId(event.getAuthUserId());
            if (profileOpt.isEmpty()) {
                log.warn("⚠️ User profile not found for auth user ID: {}", event.getAuthUserId());
                return;
            }
            
            UserProfile profile = profileOpt.get();
            
            // Verify old email matches before updating
            if (!profile.getEmail().equals(event.getOldEmail())) {
                log.warn("⚠️ Email mismatch for auth user ID: {}. Expected: {}, Found: {}", 
                        event.getAuthUserId(), event.getOldEmail(), profile.getEmail());
            }
            
            // Update email and mark as unverified (will be verified separately)
            profile.updateEmail(event.getNewEmail());
            profile.setUpdatedAt(LocalDateTime.now());
            
            userProfileRepository.save(profile);
            log.info("✅ User profile email updated from {} to {} for auth user ID: {}", 
                    event.getOldEmail(), event.getNewEmail(), event.getAuthUserId());
            
            // TODO: Publish EmailSyncCompletedEvent
            
        } catch (Exception e) {
            log.error("❌ Error handling UserEmailChangedEvent: {}", message, e);
            throw new RuntimeException("Failed to handle UserEmailChangedEvent", e);
        }
    }

    /**
     * Manual method to create user profile for testing
     * This simulates the event-driven flow without SQS
     */
    @Transactional
    public UserProfile createUserProfileManually(String authUserId, String email, String firstName, String lastName) {
        log.info("🔧 Manually creating user profile for testing - auth user ID: {}", authUserId);
        
        try {
            // Check if user profile already exists
            Optional<UserProfile> existingProfile = userProfileRepository.findByAuthUserId(java.util.UUID.fromString(authUserId));
            if (existingProfile.isPresent()) {
                log.warn("⚠️ User profile already exists for auth user ID: {}", authUserId);
                return existingProfile.get();
            }
            
            // Create user profile with duplicated email for performance
            UserProfile userProfile = UserProfile.builder()
                    .authUserId(java.util.UUID.fromString(authUserId))
                    .email(email.toLowerCase().trim()) // DUPLICATED from auth service
                    .firstName(firstName)
                    .lastName(lastName)
                    .emailVerified(false) // Will be verified separately
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            UserProfile savedProfile = userProfileRepository.save(userProfile);
            log.info("✅ User profile created manually for auth user ID: {} with email: {}", 
                    savedProfile.getAuthUserId(), savedProfile.getEmail());
            
            return savedProfile;
            
        } catch (Exception e) {
            log.error("❌ Error creating user profile manually: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create user profile manually", e);
        }
    }
}