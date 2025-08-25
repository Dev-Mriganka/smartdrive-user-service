package com.smartdrive.userservice.controller;

import com.smartdrive.userservice.dto.EmailVerificationRequest;
import com.smartdrive.userservice.dto.PasswordChangeRequest;
import com.smartdrive.userservice.dto.UserProfileUpdateRequest;
import com.smartdrive.userservice.dto.UserRegistrationRequest;
import com.smartdrive.userservice.exception.UserAlreadyExistsException;
import com.smartdrive.userservice.exception.UserNotFoundException;
import com.smartdrive.userservice.model.User;
import com.smartdrive.userservice.service.UserService;
import com.smartdrive.userservice.service.VerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for user management operations
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final VerificationService verificationService;

    /**
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerUser(
            @Valid @RequestBody UserRegistrationRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("🚀 User registration request received for username: {}", request.getUsername());
        
        try {
            String clientIp = getClientIpAddress(httpRequest);
            User user = userService.registerUser(request, clientIp);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully. Please check your email for verification.");
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("userId", user.getId());
            response.put("emailVerified", user.getIsEmailVerified());
            
            log.info("✅ User registration successful for username: {}", user.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (UserAlreadyExistsException e) {
            log.warn("❌ User registration failed - User already exists: {}", request.getUsername());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "User already exists");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            
        } catch (Exception e) {
            log.error("❌ User registration failed for username: {}", request.getUsername(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Registration failed");
            errorResponse.put("message", "An error occurred during registration");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Verify email with token
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyEmail(
            @Valid @RequestBody EmailVerificationRequest request) {
        
        log.info("🔍 Email verification request received");
        
        try {
            boolean verified = verificationService.verifyEmailToken(request.getToken());
            
            if (verified) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Email verified successfully");
                response.put("verified", true);
                
                log.info("✅ Email verification successful");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Verification failed");
                errorResponse.put("message", "Invalid or expired verification token");
                
                log.warn("❌ Email verification failed");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
        } catch (Exception e) {
            log.error("❌ Email verification failed", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Verification failed");
            errorResponse.put("message", "An error occurred during verification");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Resend verification email
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, Object>> resendVerificationEmail(@RequestParam String email) {
        log.info("📧 Resend verification email request for: {}", email);
        
        try {
            boolean sent = verificationService.resendVerificationEmail(email);
            
            if (sent) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Verification email sent successfully");
                
                log.info("✅ Verification email resent successfully to: {}", email);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Failed to send verification email");
                errorResponse.put("message", "User not found or already verified");
                
                log.warn("❌ Failed to resend verification email to: {}", email);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to resend verification email to: {}", email, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to send verification email");
            errorResponse.put("message", "An error occurred while sending verification email");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get current user profile
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<User> getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        log.info("👤 Getting profile for current user: {}", username);
        
        try {
            User user = userService.getUserByUsername(username);
            log.info("✅ Profile retrieved successfully for user: {}", username);
            return ResponseEntity.ok(user);
            
        } catch (UserNotFoundException e) {
            log.warn("❌ Profile not found for user: {}", username);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update current user profile
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> updateCurrentUserProfile(
            @Valid @RequestBody UserProfileUpdateRequest request) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        log.info("📝 Updating profile for current user: {}", username);
        
        try {
            User updatedUser = userService.updateUserProfile(username, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profile updated successfully");
            response.put("user", updatedUser);
            
            log.info("✅ Profile updated successfully for user: {}", username);
            return ResponseEntity.ok(response);
            
        } catch (UserNotFoundException e) {
            log.warn("❌ User not found: {}", username);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "User not found");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            
        } catch (Exception e) {
            log.error("❌ Profile update failed for user: {}", username, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Profile update failed");
            errorResponse.put("message", "An error occurred while updating profile");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Change current user password
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> changePassword(
            @Valid @RequestBody PasswordChangeRequest request) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        log.info("🔒 Changing password for current user: {}", username);
        
        try {
            userService.changePassword(username, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Password changed successfully");
            
            log.info("✅ Password changed successfully for user: {}", username);
            return ResponseEntity.ok(response);
            
        } catch (UserNotFoundException e) {
            log.warn("❌ User not found: {}", username);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "User not found");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            
        } catch (RuntimeException e) {
            log.warn("❌ Password change failed for user: {} - {}", username, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Password change failed");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            
        } catch (Exception e) {
            log.error("❌ Password change failed for user: {}", username, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Password change failed");
            errorResponse.put("message", "An error occurred while changing password");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
