package com.smartdrive.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for email verification requests
 */
@Data
public class EmailVerificationRequest {

    @NotBlank(message = "Verification token is required")
    private String token;
}
