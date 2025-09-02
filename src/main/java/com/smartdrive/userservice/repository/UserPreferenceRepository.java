package com.smartdrive.userservice.repository;

import com.smartdrive.userservice.model.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserPreference entity
 */
@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {

    /**
     * Find preferences by user ID
     */
    List<UserPreference> findByUserId(UUID userId);

    /**
     * Find preference by user ID and key
     */
    Optional<UserPreference> findByUserIdAndPreferenceKey(UUID userId, String preferenceKey);

    /**
     * Find preferences by key
     */
    List<UserPreference> findByPreferenceKey(String preferenceKey);

    /**
     * Find preferences by key and value containing
     */
    @Query("SELECT up FROM UserPreference up WHERE up.preferenceKey = :key AND up.preferenceValue LIKE %:value%")
    List<UserPreference> findByPreferenceKeyAndValueContaining(@Param("key") String key, @Param("value") String value);

    /**
     * Delete preference by user ID and key
     */
    void deleteByUserIdAndPreferenceKey(UUID userId, String preferenceKey);

    /**
     * Delete all preferences for user
     */
    void deleteByUserId(UUID userId);

    /**
     * Count preferences by key
     */
    long countByPreferenceKey(String preferenceKey);
}
