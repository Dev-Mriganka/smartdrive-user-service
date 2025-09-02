package com.smartdrive.userservice.controller;

import com.smartdrive.userservice.dto.UserProfileDTO;
import com.smartdrive.userservice.security.BusinessAuthorization;
import com.smartdrive.userservice.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin Controller for user management
 * Implements admin endpoints as per sequence diagram
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Management", description = "APIs for admin user management")
public class AdminController {

    private final UserProfileService userProfileService;
    private final BusinessAuthorization businessAuth;

    /**
     * Get all users with pagination (admin only)
     * Implements fast local queries as per sequence diagram
     * GET /api/admin/users?page=1&limit=50
     */
    @GetMapping("/users")
    @Operation(summary = "Get all users with pagination (admin only)")
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        log.info("📋 Admin user list request - page: {}, limit: {}", page, limit);
        
        // Check admin privileges
        businessAuth.requireAdmin();
        
        try {
            // Create pageable with sorting
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, limit, Sort.by(direction, sortBy));
            
            // Get paginated users (fast local query)
            Page<UserProfileDTO> usersPage = userProfileService.getAllUsersWithPagination(pageable);
            
            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("users", usersPage.getContent());
            response.put("totalElements", usersPage.getTotalElements());
            response.put("totalPages", usersPage.getTotalPages());
            response.put("currentPage", usersPage.getNumber());
            response.put("pageSize", usersPage.getSize());
            response.put("hasNext", usersPage.hasNext());
            response.put("hasPrevious", usersPage.hasPrevious());
            
            log.info("✅ Admin user list retrieved - {} users on page {}", 
                    usersPage.getContent().size(), page);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error retrieving admin user list", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "server_error");
            errorResponse.put("message", "Failed to retrieve user list");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Search users by email or name (admin only)
     * GET /api/admin/users/search?email=john@example.com
     */
    @GetMapping("/users/search")
    @Operation(summary = "Search users by email or name (admin only)")
    public ResponseEntity<List<UserProfileDTO>> searchUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String name) {
        
        log.info("🔍 Admin user search - email: {}, name: {}", email, name);
        
        // Check admin privileges
        businessAuth.requireAdmin();
        
        try {
            List<UserProfileDTO> users;
            
            if (email != null && !email.trim().isEmpty()) {
                users = userProfileService.searchUsersByEmail(email);
                log.info("✅ Admin search by email found {} users", users.size());
            } else if (name != null && !name.trim().isEmpty()) {
                users = userProfileService.searchUsersByName(name);
                log.info("✅ Admin search by name found {} users", users.size());
            } else {
                return ResponseEntity.badRequest().build();
            }
            
            return ResponseEntity.ok(users);
            
        } catch (Exception e) {
            log.error("❌ Error in admin user search", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get user statistics (admin only)
     * GET /api/admin/users/stats
     */
    @GetMapping("/users/stats")
    @Operation(summary = "Get user statistics (admin only)")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        log.info("📊 Admin user statistics request");
        
        // Check admin privileges
        businessAuth.requireAdmin();
        
        try {
            Map<String, Object> stats = userProfileService.getUserStatistics();
            
            log.info("✅ Admin user statistics retrieved");
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("❌ Error retrieving user statistics", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "server_error");
            errorResponse.put("message", "Failed to retrieve user statistics");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Update user status (admin only)
     * PUT /api/admin/users/{userId}/status
     */
    @PutMapping("/users/{userId}/status")
    @Operation(summary = "Update user status (admin only)")
    public ResponseEntity<Map<String, Object>> updateUserStatus(
            @PathVariable String userId,
            @RequestBody Map<String, Object> statusUpdate) {
        
        log.info("🔧 Admin user status update - userId: {}", userId);
        
        // Check admin privileges
        businessAuth.requireAdmin();
        
        try {
            Boolean enabled = (Boolean) statusUpdate.get("enabled");
            String reason = (String) statusUpdate.get("reason");
            
            if (enabled == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "invalid_request");
                errorResponse.put("message", "enabled field is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            UserProfileDTO updatedUser = userProfileService.updateUserStatus(userId, enabled, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User status updated successfully");
            response.put("user", updatedUser);
            
            log.info("✅ Admin user status updated - userId: {}, enabled: {}", userId, enabled);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error updating user status", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "server_error");
            errorResponse.put("message", "Failed to update user status");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get user details by ID (admin only)
     * GET /api/admin/users/{userId}
     */
    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user details by ID (admin only)")
    public ResponseEntity<UserProfileDTO> getUserById(@PathVariable String userId) {
        log.info("👤 Admin get user by ID - userId: {}", userId);
        
        // Check admin privileges
        businessAuth.requireAdmin();
        
        try {
            UserProfileDTO user = userProfileService.getUserProfileById(userId);
            
            log.info("✅ Admin retrieved user details - userId: {}", userId);
            return ResponseEntity.ok(user);
            
        } catch (Exception e) {
            log.error("❌ Error retrieving user by ID", e);
            return ResponseEntity.status(404).build();
        }
    }

    /**
     * Health check for admin endpoints
     */
    @GetMapping("/health")
    @Operation(summary = "Admin health check")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "admin-controller");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}