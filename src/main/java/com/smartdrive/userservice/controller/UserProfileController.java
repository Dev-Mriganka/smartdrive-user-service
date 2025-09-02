package com.smartdrive.userservice.controller;

import com.smartdrive.userservice.dto.UserProfileDTO;
import com.smartdrive.userservice.model.UserProfile;
import com.smartdrive.userservice.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for user profile management
 * Handles only user profile and business data (no authentication)
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Profile Management", description = "APIs for managing user profiles")
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * Get current user profile from JWT token
     */
    @GetMapping("/profile")
    @Operation(summary = "Get current user profile from JWT token")
    public ResponseEntity<UserProfileDTO> getCurrentUserProfile(@RequestHeader("X-User-ID") String userId) {
        log.info("Getting current user profile for user ID: {}", userId);
        try {
            UUID authUserId = UUID.fromString(userId);
            UserProfileDTO profile = userProfileService.getUserProfile(authUserId);
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format: {}", userId);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get user profile by auth user ID
     */
    @GetMapping("/profile-by-auth-id/{authUserId}")
    @Operation(summary = "Get user profile by auth user ID")
    public ResponseEntity<UserProfileDTO> getUserProfile(@PathVariable UUID authUserId) {
        log.info("Getting user profile for auth user ID: {}", authUserId);
        UserProfileDTO profile = userProfileService.getUserProfile(authUserId);
        return ResponseEntity.ok(profile);
    }

    /**
     * Get user token claims by auth user ID (used by Auth Service during login)
     * This endpoint provides the data needed for JWT token generation
     */
    @GetMapping("/profile-by-auth-id/{authUserId}/token-claims")
    @Operation(summary = "Get user token claims by auth user ID")
    public ResponseEntity<Map<String, Object>> getUserTokenClaims(@PathVariable UUID authUserId) {
        log.info("Getting user token claims for auth user ID: {}", authUserId);
        Map<String, Object> claims = userProfileService.getUserTokenClaims(authUserId);
        return ResponseEntity.ok(claims);
    }

    /**
     * Get user profile by email
     */
    @GetMapping("/profile/email/{email}")
    @Operation(summary = "Get user profile by email")
    public ResponseEntity<UserProfileDTO> getUserProfileByEmail(@PathVariable String email) {
        log.info("Getting user profile for email: {}", email);
        UserProfileDTO profile = userProfileService.getUserProfileByEmail(email);
        return ResponseEntity.ok(profile);
    }

    /**
     * Update user profile
     */
    @PutMapping("/profile/{authUserId}")
    @Operation(summary = "Update user profile")
    public ResponseEntity<UserProfileDTO> updateUserProfile(
            @PathVariable UUID authUserId,
            @RequestBody UserProfileDTO profileUpdate) {
        log.info("Updating user profile for auth user ID: {}", authUserId);
        UserProfileDTO updatedProfile = userProfileService.updateUserProfile(authUserId, profileUpdate);
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * Search users by email (fast local query as per sequence diagram)
     */
    @GetMapping("/search")
    @Operation(summary = "Search users by email or name")
    public ResponseEntity<List<UserProfileDTO>> searchUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String name) {
        
        if (email != null && !email.trim().isEmpty()) {
            log.info("Searching users by email: {}", email);
            List<UserProfileDTO> users = userProfileService.searchUsersByEmail(email);
            return ResponseEntity.ok(users);
        } else if (name != null && !name.trim().isEmpty()) {
            log.info("Searching users by name: {}", name);
            List<UserProfileDTO> users = userProfileService.searchUsersByName(name);
            return ResponseEntity.ok(users);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get all users (for admin purposes)
     */
    @GetMapping
    @Operation(summary = "Get all users")
    public ResponseEntity<List<UserProfileDTO>> getAllUsers() {
        log.info("Getting all users");
        List<UserProfileDTO> users = userProfileService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Check if user exists by email
     */
    @GetMapping("/exists/{email}")
    @Operation(summary = "Check if user exists by email")
    public ResponseEntity<Boolean> userExists(@PathVariable String email) {
        log.info("Checking if user exists with email: {}", email);
        boolean exists = userProfileService.userExists(email);
        return ResponseEntity.ok(exists);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User Service is healthy!");
    }
}
