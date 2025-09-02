package com.smartdrive.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for user profile responses in the hybrid approach
 * Contains user profile and business data (no authentication data)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private UUID id;
    private UUID authUserId;
    private String email;
    private Boolean emailVerified;
    private String firstName;
    private String lastName;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private String phone;
    private LocalDate dateOfBirth;
    private String timezone;
    private String language;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
