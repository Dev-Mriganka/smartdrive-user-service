package com.smartdrive.userservice.controller;

import com.smartdrive.userservice.exception.UserNotFoundException;
import com.smartdrive.userservice.model.Role;
import com.smartdrive.userservice.model.Role.RoleType;
import com.smartdrive.userservice.model.User;
import com.smartdrive.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller for administrative operations
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('SMARTDRIVE_ADMIN')")
public class AdminController {

    private final UserService userService;

    /**
     * Get all users
     */
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("👥 Admin requesting all users");
        
        List<User> users = userService.getAllUsers();
        
        log.info("✅ Retrieved {} users successfully", users.size());
        return ResponseEntity.ok(users);
    }

    /**
     * Get user statistics
     */
    @GetMapping("/users/statistics")
    public ResponseEntity<Map<String, Object>> getUserStatistics() {
        log.info("📊 Admin requesting user statistics");
        
        Map<String, Object> statistics = userService.getUserStatistics();
        
        log.info("✅ User statistics retrieved successfully");
        return ResponseEntity.ok(statistics);
    }

    /**
     * Toggle user account status
     */
    @PutMapping("/users/{username}/toggle")
    public ResponseEntity<Map<String, Object>> toggleUserAccount(@PathVariable String username) {
        log.info("🔄 Admin toggling user account: {}", username);
        
        try {
            User user = userService.toggleUserAccount(username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User account status updated successfully");
            response.put("username", user.getUsername());
            response.put("enabled", user.getIsEnabled());
            
            log.info("✅ User account status updated: {} (enabled: {})", username, user.getIsEnabled());
            return ResponseEntity.ok(response);
            
        } catch (UserNotFoundException e) {
            log.warn("❌ User not found: {}", username);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "User not found");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            
        } catch (Exception e) {
            log.error("❌ Error toggling user account: {}", username, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to toggle user account");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Assign roles to user
     */
    @PutMapping("/users/{username}/roles")
    public ResponseEntity<Map<String, Object>> assignRolesToUser(
            @PathVariable String username,
            @RequestBody Set<RoleType> roleTypes) {
        
        log.info("🎭 Admin assigning roles to user: {} - Roles: {}", username, roleTypes);
        
        try {
            User user = userService.assignRoles(username, roleTypes);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Roles assigned successfully");
            response.put("username", user.getUsername());
            response.put("assignedRoles", roleTypes);
            
            log.info("✅ Roles assigned successfully to user: {}", username);
            return ResponseEntity.ok(response);
            
        } catch (UserNotFoundException e) {
            log.warn("❌ User not found: {}", username);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "User not found");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            
        } catch (Exception e) {
            log.error("❌ Error assigning roles to user: {}", username, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to assign roles");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Delete user
     */
    @DeleteMapping("/users/{username}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable String username) {
        log.info("🗑️ Admin deleting user: {}", username);
        
        try {
            userService.deleteUser(username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User deleted successfully");
            response.put("username", username);
            
            log.info("✅ User deleted successfully: {}", username);
            return ResponseEntity.ok(response);
            
        } catch (UserNotFoundException e) {
            log.warn("❌ User not found: {}", username);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "User not found");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            
        } catch (Exception e) {
            log.error("❌ Error deleting user: {}", username, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to delete user");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get user by username (admin view)
     */
    @GetMapping("/users/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        log.info("👤 Admin getting user by username: {}", username);
        
        try {
            User user = userService.getUserByUsername(username);
            
            log.info("✅ User retrieved successfully: {}", username);
            return ResponseEntity.ok(user);
            
        } catch (UserNotFoundException e) {
            log.warn("❌ User not found: {}", username);
            return ResponseEntity.notFound().build();
        }
    }
}
