package com.smartdrive.userservice.service;

import com.smartdrive.userservice.dto.TokenClaimsResponse;
import com.smartdrive.userservice.model.User;
import com.smartdrive.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
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
                log.warn("❌ User account is disabled: {}", username);
                return false;
            }
            
            if (user.isAccountLocked()) {
                log.warn("❌ User account is locked: {}", username);
                return false;
            }
            
            boolean isValid = passwordEncoder.matches(password, user.getPassword());
            
            if (isValid) {
                // Reset failed login attempts on successful login
                user.resetFailedLoginAttempts();
                userRepository.save(user);
                log.info("✅ Credential verification successful for user: {}", username);
            } else {
                // Increment failed login attempts
                user.incrementFailedLoginAttempts();
                userRepository.save(user);
                log.warn("❌ Credential verification failed for user: {}", username);
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("❌ Error during credential verification for user: {}", username, e);
            return false;
        }
    }

    /**
     * Get user token claims for JWT generation
     */
    public TokenClaimsResponse getTokenClaims(String username) {
        log.info("👤 Getting token claims for user: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        List<String> roles = user.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
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
        profile.put("phoneNumber", user.getPhoneNumber());
        profile.put("bio", user.getBio());
        profile.put("profilePictureUrl", user.getProfilePictureUrl());
        profile.put("isEnabled", user.getIsEnabled());
        profile.put("isEmailVerified", user.getIsEmailVerified());
        profile.put("twoFactorEnabled", user.getTwoFactorEnabled());
        profile.put("createdAt", user.getCreatedAt());
        profile.put("updatedAt", user.getUpdatedAt());
        
        return profile;
    }
}
