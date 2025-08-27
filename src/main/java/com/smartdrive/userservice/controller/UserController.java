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
    public ResponseEntity<User> getProfile() {
        // Get user context from gateway headers (no Spring Security!)
        UserContext userContext = UserContextHolder.getAuthenticatedContext();

        // Business authorization check
        businessAuth.requireCanAccessUser(userContext.getUserId());

        log.info("🔍 Fetching profile for user: {} (requestId: {})",
                userContext.getUsername(), userContext.getRequestId());

        User user = userService.getUserByUsername(userContext.getUsername());
        return ResponseEntity.ok(user);
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
