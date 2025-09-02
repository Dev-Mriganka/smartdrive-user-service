package com.smartdrive.userservice.repository;

import com.smartdrive.userservice.model.AdminInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for admin invitation management
 */
@Repository
public interface AdminInvitationRepository extends JpaRepository<AdminInvitation, UUID> {

    /**
     * Find invitation by token
     */
    Optional<AdminInvitation> findByInvitationToken(String invitationToken);

    /**
     * Find invitation by email
     */
    Optional<AdminInvitation> findByEmail(String email);

    /**
     * Check if email has pending invitation
     */
    @Query("SELECT COUNT(ai) > 0 FROM AdminInvitation ai WHERE ai.email = :email AND ai.acceptedAt IS NULL AND ai.expiresAt > :now")
    boolean existsByEmailAndNotAcceptedAndNotExpired(@Param("email") String email, @Param("now") LocalDateTime now);

    /**
     * Find all pending invitations for an admin
     */
    @Query("SELECT ai FROM AdminInvitation ai WHERE ai.invitedBy.id = :invitedById AND ai.acceptedAt IS NULL AND ai.expiresAt > :now")
    List<AdminInvitation> findPendingInvitationsByInviter(@Param("invitedById") UUID invitedById, @Param("now") LocalDateTime now);

    /**
     * Find all pending invitations
     */
    @Query("SELECT ai FROM AdminInvitation ai WHERE ai.acceptedAt IS NULL AND ai.expiresAt > :now ORDER BY ai.createdAt DESC")
    List<AdminInvitation> findAllPendingInvitations(@Param("now") LocalDateTime now);

    /**
     * Find all invitations created by an admin
     */
    List<AdminInvitation> findByInvitedByIdOrderByCreatedAtDesc(UUID invitedById);

    /**
     * Find expired invitations that need cleanup
     */
    @Query("SELECT ai FROM AdminInvitation ai WHERE ai.expiresAt < :now AND ai.acceptedAt IS NULL")
    List<AdminInvitation> findExpiredInvitations(@Param("now") LocalDateTime now);

    /**
     * Clean up old accepted invitations (older than retention period)
     */
    @Modifying
    @Query("DELETE FROM AdminInvitation ai WHERE ai.acceptedAt IS NOT NULL AND ai.acceptedAt < :cutoffDate")
    int cleanupOldInvitations(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Count pending invitations by invited admin
     */
    @Query("SELECT COUNT(ai) FROM AdminInvitation ai WHERE ai.invitedBy.id = :invitedById AND ai.acceptedAt IS NULL AND ai.expiresAt > :now")
    int countPendingInvitationsByInviter(@Param("invitedById") UUID invitedById, @Param("now") LocalDateTime now);

    /**
     * Find invitations created in date range
     */
    @Query("SELECT ai FROM AdminInvitation ai WHERE ai.createdAt BETWEEN :startDate AND :endDate ORDER BY ai.createdAt DESC")
    List<AdminInvitation> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Find invitations by role name
     */
    List<AdminInvitation> findByRoleName(String roleName);

    /**
     * Count invitations by role name
     */
    long countByRoleName(String roleName);
}
