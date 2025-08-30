package com.smartdrive.userservice.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartdrive.userservice.dto.PasswordChangeRequest;
import com.smartdrive.userservice.dto.UserProfileUpdateRequest;
import com.smartdrive.userservice.dto.UserProfileDTO;
import com.smartdrive.userservice.model.User;
import com.smartdrive.userservice.security.BusinessAuthorization;
import com.smartdrive.userservice.security.UserContext;
import com.smartdrive.userservice.security.UserContextHolder;
import com.smartdrive.userservice.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * User Controller - Pure Business Logic
 * 
 * This controller demonstrates the infrastructure-first security approach:
 * - No Spring Security annotations (@PreAuthorize removed)
 * - Uses UserContext from API Gateway headers
 * - Business authorization rules in separate component
 * - Focus on user management business logic
 * - Easily testable without security infrastructure
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final BusinessAuthorization businessAuth;

    /**
     * Get current user's profile
     * Business Logic: Users can view their own profile, admins can view any profile
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileDTO> getProfile() {
        // Get user context from gateway headers (no Spring Security!)
        UserContext userContext = UserContextHolder.getAuthenticatedContext();

        // Business authorization check
        businessAuth.requireCanAccessUser(userContext.getUserId());

        log.info("🔍 Fetching profile for user: {} (requestId: {})",
                userContext.getUsername(), userContext.getRequestId());

        User user = userService.getUserByUsername(userContext.getUsername());
        
        // Convert to DTO to prevent circular reference issues
        UserProfileDTO profileDTO = UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .bio(user.getBio())
                .profilePictureUrl(user.getProfilePictureUrl())
                .isEnabled(user.getIsEnabled())
                .isAccountNonExpired(user.getIsAccountNonExpired())
                .isAccountNonLocked(user.getIsAccountNonLocked())
                .isCredentialsNonExpired(user.getIsCredentialsNonExpired())
                .isEmailVerified(user.getIsEmailVerified())
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .accountLockedUntil(user.getAccountLockedUntil())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roles(user.getRoleNames())
                .build();
                
        return ResponseEntity.ok(profileDTO);
    }

    /**
     * Get specific user's profile (admin endpoint)
     * Business Logic: Only admins can view other users' profiles
     */
    @GetMapping("/{userId}/profile")
    public ResponseEntity<User> getUserProfile(@PathVariable String userId) {
        UserContext userContext = UserContextHolder.getAuthenticatedContext();

        // Business authorization check
        if (!businessAuth.canViewUserProfile(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null);
        }

        log.info("🔍 Admin {} fetching profile for user: {} (requestId: {})",
                userContext.getUsername(), userId, userContext.getRequestId());

        User user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Update current user's profile
     * Business Logic: Users can update their own profile, admins can update any
     * profile
     */
    @PutMapping("/profile")
    public ResponseEntity<User> updateProfile(
            @Valid @RequestBody UserProfileUpdateRequest request) {

        UserContext userContext = UserContextHolder.getAuthenticatedContext();

        // Business authorization check
        businessAuth.requireCanAccessUser(userContext.getUserId());

        log.info("✏️ Updating profile for user: {} (requestId: {})",
                userContext.getUsername(), userContext.getRequestId());

        User updatedUser = userService.updateUserProfile(userContext.getUsername(), request);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Update specific user's profile (admin endpoint)
     */
    @PutMapping("/{userId}/profile")
    public ResponseEntity<User> updateUserProfile(
            @PathVariable String userId,
            @Valid @RequestBody UserProfileUpdateRequest request) {

        UserContext userContext = UserContextHolder.getAuthenticatedContext();

        // Business authorization check
        if (!businessAuth.canUpdateUserProfile(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null);
        }

        log.info("✏️ Admin {} updating profile for user: {} (requestId: {})",
                userContext.getUsername(), userId, userContext.getRequestId());

        // Get target user by ID and update
        User targetUser = userService.getUserById(userId);
        User updatedUser = userService.updateUserProfile(targetUser.getUsername(), request);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Change current user's password
     * Business Logic: Users can only change their own password
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody PasswordChangeRequest request) {

        UserContext userContext = UserContextHolder.getAuthenticatedContext();

        // Business authorization check (implicit - user can only change own password)
        businessAuth.requireCanAccessUser(userContext.getUserId());

        log.info("🔐 Changing password for user: {} (requestId: {})",
                userContext.getUsername(), userContext.getRequestId());

        userService.changePassword(userContext.getUsername(), request);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Password changed successfully");
        response.put("userId", userContext.getUserId());
        response.put("requestId", userContext.getRequestId());

        return ResponseEntity.ok(response);
    }

    /**
     * Delete user account (admin only)
     * Business Logic: Only admins can delete users, cannot delete themselves
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable String userId) {
        UserContext userContext = UserContextHolder.getAuthenticatedContext();

        // Business authorization check
        if (!businessAuth.canDeleteUser(userId)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Forbidden");
            error.put("message", "Cannot delete this user");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        log.info("🗑️ Admin {} deleting user: {} (requestId: {})",
                userContext.getUsername(), userId, userContext.getRequestId());

        userService.deleteUser(userId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "User deleted successfully");
        response.put("deletedUserId", userId);
        response.put("requestId", userContext.getRequestId());

        return ResponseEntity.ok(response);
    }

    /**
     * Get user by email endpoint for social login (internal use by auth service)
     */
    @GetMapping("/email/{email}/token-claims")
    public ResponseEntity<Map<String, Object>> getUserByEmailForTokenClaims(@PathVariable String email) {
        log.info("🔍 Getting user token claims by email: {}", email);
        
        try {
            Map<String, Object> tokenClaims = userService.getUserTokenClaimsByEmail(email);
            
            if (tokenClaims.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            log.info("✅ Retrieved token claims for email: {}", email);
            return ResponseEntity.ok(tokenClaims);
            
        } catch (Exception e) {
            log.error("❌ Error getting user token claims by email: {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create user from Google OAuth2 (internal use by auth service)
     */
    @PostMapping("/google-register")
    public ResponseEntity<Map<String, Object>> createGoogleUser(@RequestBody Map<String, Object> googleUserData) {
        log.info("👤 Creating user from Google OAuth2: {}", googleUserData.get("email"));
        
        try {
            Map<String, Object> userClaims = userService.createUserFromGoogleProfile(googleUserData);
            
            log.info("✅ Created user from Google OAuth2: {}", googleUserData.get("email"));
            return ResponseEntity.ok(userClaims);
            
        } catch (Exception e) {
            log.error("❌ Error creating user from Google OAuth2: {}", googleUserData.get("email"), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create user from Google profile");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || !xfHeader.contains(request.getRemoteAddr())) {
            return request.getRemoteAddr();
        } else {
            return xfHeader.split(",")[0];
        }
    }
}
