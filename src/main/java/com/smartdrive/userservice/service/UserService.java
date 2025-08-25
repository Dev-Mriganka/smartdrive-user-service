package com.smartdrive.userservice.service;

import com.smartdrive.userservice.dto.PasswordChangeRequest;
import com.smartdrive.userservice.dto.UserProfileUpdateRequest;
import com.smartdrive.userservice.dto.UserRegistrationRequest;
import com.smartdrive.userservice.exception.UserAlreadyExistsException;
import com.smartdrive.userservice.exception.UserNotFoundException;
import com.smartdrive.userservice.model.Role;
import com.smartdrive.userservice.model.Role.RoleType;
import com.smartdrive.userservice.model.User;
import com.smartdrive.userservice.repository.RoleRepository;
import com.smartdrive.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Service for user management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final VerificationService verificationService;

    /**
     * Register a new user
     */
    public User registerUser(UserRegistrationRequest request, String clientIp) {
        log.info("🚀 Registering new user: {}", request.getUsername());
        
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("❌ Username already exists: {}", request.getUsername());
            throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("❌ Email already exists: {}", request.getEmail());
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }
        
        // Get default role
        Role defaultRole = roleRepository.findByName(RoleType.SMARTDRIVE_USER)
            .orElseThrow(() -> new RuntimeException("Default role not found"));
        
        // Create user
        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phoneNumber(request.getPhoneNumber())
            .bio(request.getBio())
            .isEnabled(true)
            .isEmailVerified(false)
            .roles(Set.of(defaultRole))
            .build();
        
        User savedUser = userRepository.save(user);
        
        // Generate verification token and send email
        String verificationToken = verificationService.generateVerificationToken(savedUser);
        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getUsername(), verificationToken);
        
        log.info("✅ User registered successfully: {}", savedUser.getUsername());
        return savedUser;
    }

    /**
     * Get user by username
     */
    public User getUserByUsername(String username) {
        log.info("👤 Getting user by username: {}", username);
        
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
    }

    /**
     * Get user by email
     */
    public User getUserByEmail(String email) {
        log.info("👤 Getting user by email: {}", email);
        
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + email));
    }

    /**
     * Update user profile
     */
    public User updateUserProfile(String username, UserProfileUpdateRequest request) {
        log.info("📝 Updating profile for user: {}", username);
        
        User user = getUserByUsername(username);
        
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setBio(request.getBio());
        user.setProfilePictureUrl(request.getProfilePictureUrl());
        
        User updatedUser = userRepository.save(user);
        
        log.info("✅ Profile updated successfully for user: {}", username);
        return updatedUser;
    }

    /**
     * Change user password
     */
    public void changePassword(String username, PasswordChangeRequest request) {
        log.info("🔒 Changing password for user: {}", username);
        
        User user = getUserByUsername(username);
        
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("❌ Invalid current password for user: {}", username);
            throw new RuntimeException("Invalid current password");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        // Send notification email
        emailService.sendPasswordChangeNotification(user.getEmail(), user.getUsername());
        
        log.info("✅ Password changed successfully for user: {}", username);
    }

    /**
     * Get all users (admin only)
     */
    public List<User> getAllUsers() {
        log.info("👥 Getting all users");
        
        List<User> users = userRepository.findAll();
        log.info("✅ Retrieved {} users", users.size());
        return users;
    }

    /**
     * Toggle user account status
     */
    public User toggleUserAccount(String username) {
        log.info("🔄 Toggling account status for user: {}", username);
        
        User user = getUserByUsername(username);
        user.setIsEnabled(!user.getIsEnabled());
        
        User updatedUser = userRepository.save(user);
        
        log.info("✅ Account status toggled for user: {} (enabled: {})", username, updatedUser.getIsEnabled());
        return updatedUser;
    }

    /**
     * Assign roles to user
     */
    public User assignRoles(String username, Set<RoleType> roleTypes) {
        log.info("🎭 Assigning roles to user: {} - Roles: {}", username, roleTypes);
        
        User user = getUserByUsername(username);
        Set<Role> roles = roleTypes.stream()
            .map(roleType -> roleRepository.findByName(roleType)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleType)))
            .collect(java.util.stream.Collectors.toSet());
        
        user.setRoles(roles);
        User updatedUser = userRepository.save(user);
        
        log.info("✅ Roles assigned successfully to user: {}", username);
        return updatedUser;
    }

    /**
     * Get user statistics
     */
    public Map<String, Object> getUserStatistics() {
        log.info("📊 Getting user statistics");
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalUsers", userRepository.count());
        statistics.put("verifiedUsers", userRepository.countByIsEmailVerified(true));
        statistics.put("unverifiedUsers", userRepository.countByIsEmailVerified(false));
        statistics.put("enabledUsers", userRepository.findByIsEnabledTrue().size());
        statistics.put("usersWithFailedLogins", userRepository.findByFailedLoginAttemptsGreaterThan(0).size());
        
        log.info("✅ User statistics retrieved successfully");
        return statistics;
    }

    /**
     * Handle failed login attempt
     */
    public void handleFailedLogin(String username) {
        log.info("🚫 Handling failed login for user: {}", username);
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.incrementFailedLoginAttempts();
            userRepository.save(user);
            
            if (user.isAccountLocked()) {
                emailService.sendAccountLockNotification(user.getEmail(), user.getUsername(), "Multiple failed login attempts");
            }
        }
    }

    /**
     * Handle successful login
     */
    public void handleSuccessfulLogin(String username) {
        log.info("✅ Handling successful login for user: {}", username);
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.resetFailedLoginAttempts();
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    /**
     * Check if user exists by username
     */
    public boolean userExistsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Check if user exists by email
     */
    public boolean userExistsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Delete user
     */
    public void deleteUser(String username) {
        log.info("🗑️ Deleting user: {}", username);
        
        User user = getUserByUsername(username);
        userRepository.delete(user);
        
        log.info("✅ User deleted successfully: {}", username);
    }
}
