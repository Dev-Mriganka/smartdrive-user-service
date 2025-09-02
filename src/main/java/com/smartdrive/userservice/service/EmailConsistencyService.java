package com.smartdrive.userservice.service;

import java.util.List;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartdrive.userservice.client.AuthServiceClient;
import com.smartdrive.userservice.model.UserProfile;
import com.smartdrive.userservice.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for monitoring and maintaining email consistency between Auth and
 * User services
 * Runs periodic checks and fixes inconsistencies
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmailConsistencyService {

    private final UserProfileRepository userProfileRepository;
    private final AuthServiceClient authServiceClient;
    private final AuditService auditService;

    /**
     * Daily consistency check job
     * Compares emails between Auth Service and User Service
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    public void performDailyConsistencyCheck() {
        log.info("🔍 Starting daily email consistency check...");

        try {
            List<UserProfile> allProfiles = userProfileRepository.findAll();
            int checkedCount = 0;
            int fixedCount = 0;

            for (UserProfile profile : allProfiles) {
                try {
                    boolean isConsistent = checkEmailConsistency(profile);
                    checkedCount++;

                    if (!isConsistent) {
                        fixEmailInconsistency(profile);
                        fixedCount++;
                    }

                } catch (Exception e) {
                    log.error("❌ Failed to check consistency for user: {}", profile.getAuthUserId(), e);
                }
            }

            log.info("✅ Daily consistency check completed. Checked: {}, Fixed: {}", checkedCount, fixedCount);

            // Audit the consistency check
            auditService.logSystemAction(
                    "EMAIL_CONSISTENCY_CHECK",
                    "Daily email consistency check completed",
                    String.format("Checked: %d, Fixed: %d", checkedCount, fixedCount));

        } catch (Exception e) {
            log.error("❌ Daily consistency check failed", e);
        }
    }

    /**
     * Check if email is consistent between Auth and User services
     */
    public boolean checkEmailConsistency(UserProfile profile) {
        try {
            // Get canonical email from Auth Service
            String authEmail = authServiceClient.getUserEmail(profile.getAuthUserId());
            String userEmail = profile.getEmail();

            boolean isConsistent = authEmail.equals(userEmail);

            if (!isConsistent) {
                log.warn("⚠️ Email inconsistency detected for user: {}. Auth: {}, User: {}",
                        profile.getAuthUserId(), authEmail, userEmail);
            }

            return isConsistent;

        } catch (Exception e) {
            log.error("❌ Failed to check email consistency for user: {}", profile.getAuthUserId(), e);
            return false;
        }
    }

    /**
     * Fix email inconsistency by updating User Service with Auth Service data
     */
    public void fixEmailInconsistency(UserProfile profile) {
        try {
            // Get canonical data from Auth Service
            String authEmail = authServiceClient.getUserEmail(profile.getAuthUserId());
            Boolean authEmailVerified = authServiceClient.getUserEmailVerified(profile.getAuthUserId());

            // Update User Service with Auth Service data
            profile.setEmail(authEmail);
            profile.setEmailVerified(authEmailVerified);
            userProfileRepository.save(profile);

            // Audit the fix
            auditService.logUserAction(
                    profile.getId(),
                    "EMAIL_CONSISTENCY_FIXED",
                    "Email inconsistency fixed with Auth Service data",
                    String.format("Updated email to: %s, verified: %s", authEmail, authEmailVerified));

            log.info("✅ Fixed email inconsistency for user: {}. Updated to: {}",
                    profile.getAuthUserId(), authEmail);

        } catch (Exception e) {
            log.error("❌ Failed to fix email inconsistency for user: {}", profile.getAuthUserId(), e);
            throw e;
        }
    }

    /**
     * Manual consistency check for a specific user
     */
    public boolean checkUserEmailConsistency(UUID authUserId) {
        UserProfile profile = userProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new RuntimeException("User profile not found: " + authUserId));

        return checkEmailConsistency(profile);
    }

    /**
     * Manual fix for a specific user
     */
    public void fixUserEmailConsistency(UUID authUserId) {
        UserProfile profile = userProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new RuntimeException("User profile not found: " + authUserId));

        fixEmailInconsistency(profile);
    }

    /**
     * Get consistency statistics
     */
    public EmailConsistencyStats getConsistencyStats() {
        List<UserProfile> allProfiles = userProfileRepository.findAll();
        int totalProfiles = allProfiles.size();
        int consistentProfiles = 0;
        int inconsistentProfiles = 0;

        for (UserProfile profile : allProfiles) {
            try {
                if (checkEmailConsistency(profile)) {
                    consistentProfiles++;
                } else {
                    inconsistentProfiles++;
                }
            } catch (Exception e) {
                log.warn("Failed to check consistency for user: {}", profile.getAuthUserId());
                inconsistentProfiles++;
            }
        }

        return EmailConsistencyStats.builder()
                .totalProfiles(totalProfiles)
                .consistentProfiles(consistentProfiles)
                .inconsistentProfiles(inconsistentProfiles)
                .consistencyPercentage(totalProfiles > 0 ? (double) consistentProfiles / totalProfiles * 100 : 0)
                .build();
    }

    /**
     * Statistics for email consistency
     */
    public static class EmailConsistencyStats {
        private int totalProfiles;
        private int consistentProfiles;
        private int inconsistentProfiles;
        private double consistencyPercentage;

        // Builder pattern
        public static EmailConsistencyStatsBuilder builder() {
            return new EmailConsistencyStatsBuilder();
        }

        public static class EmailConsistencyStatsBuilder {
            private EmailConsistencyStats stats = new EmailConsistencyStats();

            public EmailConsistencyStatsBuilder totalProfiles(int totalProfiles) {
                stats.totalProfiles = totalProfiles;
                return this;
            }

            public EmailConsistencyStatsBuilder consistentProfiles(int consistentProfiles) {
                stats.consistentProfiles = consistentProfiles;
                return this;
            }

            public EmailConsistencyStatsBuilder inconsistentProfiles(int inconsistentProfiles) {
                stats.inconsistentProfiles = inconsistentProfiles;
                return this;
            }

            public EmailConsistencyStatsBuilder consistencyPercentage(double consistencyPercentage) {
                stats.consistencyPercentage = consistencyPercentage;
                return this;
            }

            public EmailConsistencyStats build() {
                return stats;
            }
        }

        // Getters
        public int getTotalProfiles() {
            return totalProfiles;
        }

        public int getConsistentProfiles() {
            return consistentProfiles;
        }

        public int getInconsistentProfiles() {
            return inconsistentProfiles;
        }

        public double getConsistencyPercentage() {
            return consistencyPercentage;
        }
    }
}
