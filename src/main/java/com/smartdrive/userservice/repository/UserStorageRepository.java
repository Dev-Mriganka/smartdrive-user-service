package com.smartdrive.userservice.repository;

import com.smartdrive.userservice.model.UserStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserStorage entity
 */
@Repository
public interface UserStorageRepository extends JpaRepository<UserStorage, UUID> {

    /**
     * Find storage by user ID
     */
    Optional<UserStorage> findByUserId(UUID userId);

    /**
     * Find users with storage usage above threshold
     */
    @Query("SELECT us FROM UserStorage us WHERE us.storageUsed > :threshold")
    List<UserStorage> findUsersWithHighStorageUsage(@Param("threshold") Long threshold);

    /**
     * Find users with storage usage percentage above threshold
     */
    @Query("SELECT us FROM UserStorage us WHERE (us.storageUsed * 100.0 / us.storageLimit) > :percentage")
    List<UserStorage> findUsersWithHighStoragePercentage(@Param("percentage") Double percentage);

    /**
     * Find users with available storage below threshold
     */
    @Query("SELECT us FROM UserStorage us WHERE (us.storageLimit - us.storageUsed) < :availableBytes")
    List<UserStorage> findUsersWithLowAvailableStorage(@Param("availableBytes") Long availableBytes);

    /**
     * Count users with storage usage above threshold
     */
    @Query("SELECT COUNT(us) FROM UserStorage us WHERE us.storageUsed > :threshold")
    long countUsersWithHighStorageUsage(@Param("threshold") Long threshold);

    /**
     * Get total storage used across all users
     */
    @Query("SELECT SUM(us.storageUsed) FROM UserStorage us")
    Long getTotalStorageUsed();

    /**
     * Get total storage limit across all users
     */
    @Query("SELECT SUM(us.storageLimit) FROM UserStorage us")
    Long getTotalStorageLimit();

    /**
     * Get average storage usage per user
     */
    @Query("SELECT AVG(us.storageUsed) FROM UserStorage us")
    Double getAverageStorageUsage();
}
