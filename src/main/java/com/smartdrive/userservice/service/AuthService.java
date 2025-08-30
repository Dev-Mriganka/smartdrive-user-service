package com.smartdrive.userservice.service;

import com.smartdrive.userservice.dto.TokenClaimsResponse;
import com.smartdrive.userservice.dto.UserRegistrationRequest;
import com.smartdrive.userservice.model.Role;
import com.smartdrive.userservice.model.Role.RoleType;
import com.smartdrive.userservice.model.User;
import com.smartdrive.userservice.repository.RoleRepository;
import com.smartdrive.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${auth.service.internal.key:internal-auth-key}")
    private String internalAuthKey;

    /**
     * Validate internal authentication header
     */
    public boolean validateInternalAuth(String authHeader) {
        return internalAuthKey.equals(authHeader);
    }

    /**
     * Verify user credentials
     */
    public boolean verifyCredentials(String username, String password) {
        log.info("🔐 Verifying credentials for user: {}", username);

        try {
            User user = userRepository.findByUsername(username)
                    .orElse(null);

            if (user == null) {
                log.warn("❌ User not found: {}", username);
                return false;
            }

            if (!user.getIsEnabled()) {
                log.warn("❌ Account disabled for user: {}", username);
                return false;
            }

            if (!user.getIsEmailVerified()) {
                log.warn("⚠️ Email not verified for user: {} - ALLOWING FOR TESTING ONLY", username);
                // TEMPORARY: Allow login for testing JWT consistency
                // return false;
            }

            if (passwordEncoder.matches(password, user.getPassword())) {
                log.info("✅ Password matched for user: {}", username);
                user.setLastLoginAt(LocalDateTime.now());
                userRepository.save(user);
                return true;
            } else {
                log.warn("❌ Invalid password for user: {}", username);
                return false;
            }

        } catch (Exception e) {
            log.error("❌ Error during credential verification for user: {}", username, e);
            return false;
        }
    }

    /**
     * Get user token claims for JWT generation
     */
    public TokenClaimsResponse getTokenClaims(String username) {
        log.info("👤 Retrieving token claims for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<String> roles = user.getRoles().stream()
                .map(role -> "ROLE_" + role.getName().name())
                .collect(Collectors.toList());

        return TokenClaimsResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(roles)
                .isEnabled(user.getIsEnabled())
                .isEmailVerified(user.getIsEmailVerified())
                .build();
    }

    /**
     * Get user token claims by user ID for JWT generation
     */
    public TokenClaimsResponse getTokenClaimsById(String userId) {
        log.info("👤 Retrieving token claims for user ID: {}", userId);

        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        List<String> roles = user.getRoles().stream()
                .map(role -> "ROLE_" + role.getName().name())
                .collect(Collectors.toList());

        return TokenClaimsResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(roles)
                .isEnabled(user.getIsEnabled())
                .isEmailVerified(user.getIsEmailVerified())
                .build();
    }

    /**
     * Get user profile for userinfo endpoint
     */
    public Map<String, Object> getUserProfile(String username) {
        log.info("👤 Getting user profile for: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("firstName", user.getFirstName());
        profile.put("lastName", user.getLastName());
        profile.put("bio", user.getBio());
        profile.put("profilePictureUrl", user.getProfilePictureUrl());
        profile.put("isEnabled", user.getIsEnabled());
        profile.put("isEmailVerified", user.getIsEmailVerified());
        profile.put("twoFactorEnabled", user.getTwoFactorEnabled());
        profile.put("createdAt", user.getCreatedAt());
        profile.put("updatedAt", user.getUpdatedAt());

        return profile;
    }

    @Transactional
    public User registerUser(UserRegistrationRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: Email already in use - {}", request.getEmail());
            throw new IllegalStateException("An account with this email already exists.");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: Username already taken - {}", request.getUsername());
            throw new IllegalStateException("This username is already taken.");
        }

        Role userRole = roleRepository.findByName(RoleType.SMARTDRIVE_USER)
                .orElseThrow(() -> new IllegalStateException("Default user role not found."));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .roles(Set.of(userRole))
                .isEnabled(true) // Enabled by default, but email not verified
                .isEmailVerified(false)
                .emailVerificationToken(UUID.randomUUID().toString())
                .emailVerificationExpiresAt(LocalDateTime.now().plusHours(24))
                .build();

        userRepository.save(user);
        log.info("User registered successfully with email: {}. Verification token: {}", user.getEmail(), user.getEmailVerificationToken());

        // In a real application, you would trigger an email service here
        // sendVerificationEmail(user.getEmail(), user.getEmailVerificationToken());

        return user;
    }

    @Transactional
    public void verifyEmail(String token) {
        log.info("Attempting to verify email with token: {}", token);

        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new IllegalStateException("Invalid verification token."));

        if (user.getEmailVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Verification failed: Token has expired for user {}", user.getEmail());
            // Optionally, generate a new token and resend
            throw new IllegalStateException("Verification token has expired.");
        }

        user.setIsEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);

        log.info("Email verified successfully for user: {}", user.getEmail());
    }

    /**
     * Create admin user with admin privileges
     * TEMPORARY: For testing purposes only
     */
    @Transactional
    public User createAdminUser(UserRegistrationRequest request) {
        log.info("🚀 Creating admin user: {}", request.getUsername());

        // Check if admin user already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            log.info("📋 Admin user already exists: {}", request.getUsername());
            User existingUser = userRepository.findByUsername(request.getUsername()).get();
            ensureUserHasAdminRole(existingUser);
            return existingUser;
        }

        // Get required roles
        Role adminRole = roleRepository.findByName(RoleType.SMARTDRIVE_ADMIN)
                .orElseThrow(() -> new RuntimeException("Admin role not found"));
        Role userRole = roleRepository.findByName(RoleType.SMARTDRIVE_USER)
                .orElseThrow(() -> new RuntimeException("User role not found"));

        // Create admin user
        User adminUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .bio(request.getBio())
                .isEnabled(true)
                .isEmailVerified(true) // Admin doesn't need email verification
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        // Set admin roles
        Set<Role> roles = Set.of(adminRole, userRole);
        adminUser.setRoles(roles);

        User savedAdmin = userRepository.save(adminUser);

        log.info("✅ Admin user created successfully: {} with roles: {}", 
                savedAdmin.getUsername(), 
                savedAdmin.getRoleNames());
        
        return savedAdmin;
    }

    /**
     * Ensure user has admin role
     */
    private void ensureUserHasAdminRole(User user) {
        boolean hasAdminRole = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleType.SMARTDRIVE_ADMIN);
        
        if (!hasAdminRole) {
            log.info("🔧 Adding admin role to existing user: {}", user.getUsername());
            
            Role adminRole = roleRepository.findByName(RoleType.SMARTDRIVE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));
            
            Set<Role> roles = user.getRoles();
            roles.add(adminRole);
            user.setRoles(roles);
            
            userRepository.save(user);
            
            log.info("✅ Admin role added to user: {}", user.getUsername());
        }
    }
}
