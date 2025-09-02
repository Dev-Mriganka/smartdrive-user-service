package com.smartdrive.userservice.repository;

import com.smartdrive.userservice.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserProfile entity
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    /**
     * Find profile by auth user ID
     */
    Optional<UserProfile> findByAuthUserId(UUID authUserId);

    /**
     * Find profile by email (for performance - no API calls needed)
     */
    Optional<UserProfile> findByEmail(String email);

    /**
     * Check if email exists (for performance)
     */
    Boolean existsByEmail(String email);

    /**
     * Find profiles by display name
     */
    List<UserProfile> findByDisplayNameContainingIgnoreCase(String displayName);

    /**
     * Find profiles by first name or last name
     */
    @Query("SELECT up FROM UserProfile up WHERE LOWER(up.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(up.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<UserProfile> findByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Find profiles by email containing (for search functionality)
     */
    List<UserProfile> findByEmailContainingIgnoreCase(String email);

    /**
     * Find profiles by verification status
     */
    List<UserProfile> findByEmailVerified(Boolean emailVerified);

    /**
     * Find profiles created after a specific date
     */
    List<UserProfile> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find profiles by timezone
     */
    List<UserProfile> findByTimezone(String timezone);

    /**
     * Find profiles by language
     */
    List<UserProfile> findByLanguage(String language);

    /**
     * Count profiles by language
     */
    long countByLanguage(String language);

    /**
     * Count profiles by timezone
     */
    long countByTimezone(String timezone);

    /**
     * Count profiles by verification status
     */
    long countByEmailVerified(Boolean emailVerified);

    /**
     * Find profiles with email verification issues (for consistency checks)
     */
    @Query("SELECT up FROM UserProfile up WHERE up.emailVerified = false AND up.createdAt < :cutoffDate")
    List<UserProfile> findUnverifiedProfilesOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}
