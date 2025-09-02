package com.smartdrive.userservice.service;

import com.smartdrive.userservice.dto.UserProfileDTO;
import com.smartdrive.userservice.model.UserProfile;
import com.smartdrive.userservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for user profile management
 * Handles only user profile and business data (no authentication)
 * Implements caching for performance as per sequence diagram
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    /**
     * Get user profile by auth user ID with caching
     * Implements the cache hit/miss flow from sequence diagram
     */
    @Cacheable(value = "userProfile", key = "#authUserId")
    public UserProfileDTO getUserProfile(UUID authUserId) {
        log.info("Getting user profile for auth user ID: {}", authUserId);
        
        UserProfile profile = userProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new RuntimeException("User profile not found for auth user ID: " + authUserId));
        
        return mapToDTO(profile);
    }

    /**
     * Get user profile by auth user ID for token claims (used by Auth Service)
     * Returns data needed for JWT token generation
     */
    @Cacheable(value = "userProfile", key = "#authUserId")
    public Map<String, Object> getUserTokenClaims(UUID authUserId) {
        log.info("Getting user token claims for auth user ID: {}", authUserId);
        
        UserProfile profile = userProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new RuntimeException("User profile not found for auth user ID: " + authUserId));
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", profile.getAuthUserId().toString());
        claims.put("email", profile.getEmail());
        claims.put("first_name", profile.getFirstName());
        claims.put("last_name", profile.getLastName());
        claims.put("name", profile.getFullName());
        claims.put("roles", List.of("user")); // Default role, can be enhanced
        claims.put("subscription", "free"); // Default subscription, can be enhanced
        claims.put("is_email_verified", profile.getEmailVerified());
        claims.put("is_enabled", true); // Default enabled, can be enhanced
        
        return claims;
    }

    /**
     * Get user profile by email
     */
    public UserProfileDTO getUserProfileByEmail(String email) {
        log.info("Getting user profile for email: {}", email);
        
        UserProfile profile = userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User profile not found for email: " + email));
        
        return mapToDTO(profile);
    }

    /**
     * Update user profile
     */
    public UserProfileDTO updateUserProfile(UUID authUserId, UserProfileDTO profileUpdate) {
        log.info("Updating user profile for auth user ID: {}", authUserId);
        
        UserProfile profile = userProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new RuntimeException("User profile not found for auth user ID: " + authUserId));
        
        // Update profile fields (but not email - that's managed by Auth Service)
        if (profileUpdate.getFirstName() != null) {
            profile.setFirstName(profileUpdate.getFirstName());
        }
        if (profileUpdate.getLastName() != null) {
            profile.setLastName(profileUpdate.getLastName());
        }
        if (profileUpdate.getDisplayName() != null) {
            profile.setDisplayName(profileUpdate.getDisplayName());
        }
        if (profileUpdate.getAvatarUrl() != null) {
            profile.setAvatarUrl(profileUpdate.getAvatarUrl());
        }
        if (profileUpdate.getBio() != null) {
            profile.setBio(profileUpdate.getBio());
        }
        if (profileUpdate.getPhone() != null) {
            profile.setPhone(profileUpdate.getPhone());
        }
        if (profileUpdate.getDateOfBirth() != null) {
            profile.setDateOfBirth(profileUpdate.getDateOfBirth());
        }
        if (profileUpdate.getTimezone() != null) {
            profile.setTimezone(profileUpdate.getTimezone());
        }
        if (profileUpdate.getLanguage() != null) {
            profile.setLanguage(profileUpdate.getLanguage());
        }
        
        UserProfile savedProfile = userProfileRepository.save(profile);
        return mapToDTO(savedProfile);
    }

    /**
     * Search users by email (fast local query as per sequence diagram)
     */
    public List<UserProfileDTO> searchUsersByEmail(String email) {
        log.info("Searching users by email: {}", email);
        
        List<UserProfile> profiles = userProfileRepository.findByEmailContainingIgnoreCase(email);
        return profiles.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Search users by name
     */
    public List<UserProfileDTO> searchUsersByName(String name) {
        log.info("Searching users by name: {}", name);
        
        List<UserProfile> profiles = userProfileRepository.findByNameContainingIgnoreCase(name);
        return profiles.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all users with pagination (for admin purposes)
     * Implements fast local queries as per sequence diagram
     */
    public Page<UserProfileDTO> getAllUsersWithPagination(Pageable pageable) {
        log.info("Getting all users with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        
        Page<UserProfile> profiles = userProfileRepository.findAll(pageable);
        return profiles.map(this::mapToDTO);
    }

    /**
     * Get all users (legacy method)
     */
    public List<UserProfileDTO> getAllUsers() {
        log.info("Getting all users");
        
        List<UserProfile> profiles = userProfileRepository.findAll();
        return profiles.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get user statistics (for admin dashboard)
     */
    public Map<String, Object> getUserStatistics() {
        log.info("Getting user statistics");
        
        long totalUsers = userProfileRepository.count();
        long verifiedUsers = userProfileRepository.countByEmailVerified(true);
        long unverifiedUsers = totalUsers - verifiedUsers;
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("verifiedUsers", verifiedUsers);
        stats.put("unverifiedUsers", unverifiedUsers);
        stats.put("verificationRate", totalUsers > 0 ? (double) verifiedUsers / totalUsers * 100 : 0);
        
        return stats;
    }

    /**
     * Update user status (enable/disable)
     */
    public UserProfileDTO updateUserStatus(String userId, Boolean enabled, String reason) {
        log.info("Updating user status - userId: {}, enabled: {}", userId, enabled);
        
        UserProfile profile = userProfileRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User profile not found for ID: " + userId));
        
        // Note: In a full implementation, you'd have an 'enabled' field in UserProfile
        // For now, we'll just log the action
        log.info("User status update - userId: {}, enabled: {}, reason: {}", userId, enabled, reason);
        
        return mapToDTO(profile);
    }

    /**
     * Get user profile by ID
     */
    public UserProfileDTO getUserProfileById(String userId) {
        log.info("Getting user profile by ID: {}", userId);
        
        UserProfile profile = userProfileRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User profile not found for ID: " + userId));
        
        return mapToDTO(profile);
    }

    /**
     * Check if user exists by email
     */
    public boolean userExists(String email) {
        log.info("Checking if user exists with email: {}", email);
        return userProfileRepository.existsByEmail(email);
    }

    /**
     * Create user profile (called by event consumer)
     */
    public UserProfile createUserProfile(UUID authUserId, String email, String firstName, String lastName, Boolean emailVerified) {
        log.info("Creating user profile for auth user ID: {}", authUserId);
        
        UserProfile profile = UserProfile.builder()
                .authUserId(authUserId)
                .email(email)
                .emailVerified(emailVerified)
                .firstName(firstName)
                .lastName(lastName)
                .displayName(firstName + " " + lastName)
                .build();
        
        return userProfileRepository.save(profile);
    }

    /**
     * Update email (called by event consumer)
     */
    public UserProfile updateEmail(UUID authUserId, String newEmail, Boolean emailVerified) {
        log.info("Updating email for auth user ID: {} to: {}", authUserId, newEmail);
        
        UserProfile profile = userProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new RuntimeException("User profile not found for auth user ID: " + authUserId));
        
        profile.updateEmail(newEmail);
        profile.setEmailVerified(emailVerified);
        
        return userProfileRepository.save(profile);
    }

    /**
     * Mark email as verified (called by event consumer)
     */
    public UserProfile markEmailVerified(UUID authUserId) {
        log.info("Marking email as verified for auth user ID: {}", authUserId);
        
        UserProfile profile = userProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new RuntimeException("User profile not found for auth user ID: " + authUserId));
        
        profile.markEmailVerified();
        return userProfileRepository.save(profile);
    }

    /**
     * Map UserProfile entity to DTO
     */
    private UserProfileDTO mapToDTO(UserProfile profile) {
        return UserProfileDTO.builder()
                .id(profile.getId())
                .authUserId(profile.getAuthUserId())
                .email(profile.getEmail())
                .emailVerified(profile.getEmailVerified())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .displayName(profile.getDisplayName())
                .avatarUrl(profile.getAvatarUrl())
                .bio(profile.getBio())
                .phone(profile.getPhone())
                .dateOfBirth(profile.getDateOfBirth())
                .timezone(profile.getTimezone())
                .language(profile.getLanguage())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
