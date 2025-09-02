package com.smartdrive.userservice.repository;

import com.smartdrive.userservice.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for UserRole entity
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    /**
     * Find roles by user ID
     */
    List<UserRole> findByUserId(UUID userId);

    /**
     * Find roles by user ID and role name
     */
    List<UserRole> findByUserIdAndRoleName(UUID userId, String roleName);

    /**
     * Find valid roles by user ID
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.user.id = :userId AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)")
    List<UserRole> findValidRolesByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /**
     * Find roles by role name
     */
    List<UserRole> findByRoleName(String roleName);

    /**
     * Find valid roles by role name
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.roleName = :roleName AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)")
    List<UserRole> findValidRolesByRoleName(@Param("roleName") String roleName, @Param("now") LocalDateTime now);

    /**
     * Find expired roles
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.expiresAt IS NOT NULL AND ur.expiresAt < :now")
    List<UserRole> findExpiredRoles(@Param("now") LocalDateTime now);

    /**
     * Count roles by role name
     */
    long countByRoleName(String roleName);

    /**
     * Count valid roles by role name
     */
    @Query("SELECT COUNT(ur) FROM UserRole ur WHERE ur.roleName = :roleName AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)")
    long countValidRolesByRoleName(@Param("roleName") String roleName, @Param("now") LocalDateTime now);
}
