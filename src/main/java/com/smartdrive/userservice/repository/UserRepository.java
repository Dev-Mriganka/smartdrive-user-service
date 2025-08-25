package com.smartdrive.userservice.repository;

import com.smartdrive.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find user by email verification token
     */
    Optional<User> findByEmailVerificationToken(String token);

    /**
     * Find users with expired verification tokens
     */
    @Query("SELECT u FROM User u WHERE u.emailVerificationExpiresAt < :now AND u.isEmailVerified = false")
    List<User> findUsersWithExpiredVerificationTokens(@Param("now") LocalDateTime now);

    /**
     * Find users by role
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleType")
    List<User> findByRole(@Param("roleType") String roleType);

    /**
     * Find enabled users
     */
    List<User> findByIsEnabledTrue();

    /**
     * Find users created after a specific date
     */
    List<User> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Count users by verification status
     */
    long countByIsEmailVerified(Boolean isEmailVerified);

    /**
     * Find users with failed login attempts
     */
    List<User> findByFailedLoginAttemptsGreaterThan(Integer attempts);
}
