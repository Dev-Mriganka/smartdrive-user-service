package com.smartdrive.userservice.service;

import com.smartdrive.userservice.model.User;
import com.smartdrive.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling email verification
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * Generate verification token for user
     */
    public String generateVerificationToken(User user) {
        log.info("🔐 Generating verification token for user: {}", user.getUsername());
        
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
        
        user.setEmailVerificationToken(token);
        user.setEmailVerificationExpiresAt(expiresAt);
        userRepository.save(user);
        
        log.info("✅ Verification token generated for user: {}", user.getUsername());
        return token;
    }

    /**
     * Verify email token
     */
    public boolean verifyEmailToken(String token) {
        log.info("🔍 Verifying email token: {}", token);
        
        Optional<User> userOpt = userRepository.findByEmailVerificationToken(token);
        
        if (userOpt.isEmpty()) {
            log.warn("❌ Invalid verification token: {}", token);
            return false;
        }
        
        User user = userOpt.get();
        
        // Check if token is expired
        if (user.getEmailVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("❌ Expired verification token for user: {}", user.getUsername());
            return false;
        }
        
        // Check if already verified
        if (user.getIsEmailVerified()) {
            log.warn("❌ User already verified: {}", user.getUsername());
            return false;
        }
        
        // Mark as verified
        user.setIsEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);
        
        log.info("✅ Email verified successfully for user: {}", user.getUsername());
        
        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
        
        return true;
    }

    /**
     * Resend verification email
     */
    public boolean resendVerificationEmail(String email) {
        log.info("📧 Resending verification email to: {}", email);
        
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            log.warn("❌ User not found for email: {}", email);
            return false;
        }
        
        User user = userOpt.get();
        
        // Check if already verified
        if (user.getIsEmailVerified()) {
            log.warn("❌ User already verified: {}", user.getUsername());
            return false;
        }
        
        // Generate new token
        String newToken = generateVerificationToken(user);
        
        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), newToken);
        
        log.info("✅ Verification email resent to: {}", email);
        return true;
    }

    /**
     * Clean up expired verification tokens
     */
    public void cleanupExpiredTokens() {
        log.info("🧹 Cleaning up expired verification tokens");
        
        LocalDateTime now = LocalDateTime.now();
        var expiredUsers = userRepository.findUsersWithExpiredVerificationTokens(now);
        
        expiredUsers.forEach(user -> {
            user.setEmailVerificationToken(null);
            user.setEmailVerificationExpiresAt(null);
            userRepository.save(user);
            log.debug("🧹 Cleaned up expired token for user: {}", user.getUsername());
        });
        
        log.info("✅ Cleaned up {} expired verification tokens", expiredUsers.size());
    }

    /**
     * Check if user is verified
     */
    public boolean isUserVerified(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        return userOpt.map(User::getIsEmailVerified).orElse(false);
    }
}
