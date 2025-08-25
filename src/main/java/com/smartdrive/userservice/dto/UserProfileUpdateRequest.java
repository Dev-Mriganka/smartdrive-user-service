package com.smartdrive.userservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO for user profile update requests
 */
@Data
public class UserProfileUpdateRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must be less than 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must be less than 100 characters")
    private String lastName;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be valid")
    private String phoneNumber;

    @Size(max = 500, message = "Bio must be less than 500 characters")
    private String bio;

    @Pattern(regexp = "^https?://.*", message = "Profile picture URL must be a valid HTTP/HTTPS URL")
    private String profilePictureUrl;
}
