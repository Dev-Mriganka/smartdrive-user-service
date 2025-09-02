package com.smartdrive.userservice.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserProfile entity for user service
 * Handles only user profile and business data
 * Authentication is managed by Auth Service
 * Email is duplicated here for performance (hybrid approach)
 */
@Entity
@Table(name = "user_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "auth_user_id", unique = true, nullable = false)
    private UUID authUserId; // References auth_service.auth_users.id

    @Column(nullable = false)
    private String email; // Duplicated from auth_service for performance

    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false; // Sync verification status from auth_service

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    private String bio;

    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Builder.Default
    private String timezone = "UTC";

    @Builder.Default
    private String language = "en";

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else {
            return displayName != null ? displayName : "Unknown User";
        }
    }

    public String getDisplayNameOrDefault() {
        return displayName != null ? displayName : getFullName();
    }

    // Email sync methods
    public void updateEmail(String newEmail) {
        this.email = newEmail;
        this.emailVerified = false; // Require re-verification when email changes
    }

    public void markEmailVerified() {
        this.emailVerified = true;
    }
}
