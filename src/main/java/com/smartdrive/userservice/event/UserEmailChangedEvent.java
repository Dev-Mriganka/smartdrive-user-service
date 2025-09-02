package com.smartdrive.userservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a user's email is changed in the auth service
 * This event is consumed by the user service to sync email changes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEmailChangedEvent {
    private UUID authUserId;
    private String oldEmail;
    private String newEmail;
    private LocalDateTime timestamp;
    
    @Builder.Default
    private String eventType = "USER_EMAIL_CHANGED";
}