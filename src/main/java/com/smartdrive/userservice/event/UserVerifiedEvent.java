package com.smartdrive.userservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a user's email is verified in the auth service
 * This event is consumed by the user service to update verification status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVerifiedEvent {
    private UUID authUserId;
    private String email;
    private LocalDateTime timestamp;
    
    @Builder.Default
    private String eventType = "USER_VERIFIED";
}