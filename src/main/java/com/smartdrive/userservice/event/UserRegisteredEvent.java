package com.smartdrive.userservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a user registers in the auth service
 * This event is consumed by the user service to create user profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {
    private UUID authUserId;
    private String email;
    private String firstName;
    private String lastName;
    private boolean emailVerified;
    private String provider;
    private LocalDateTime timestamp;
    
    @Builder.Default
    private String eventType = "USER_REGISTERED";
}