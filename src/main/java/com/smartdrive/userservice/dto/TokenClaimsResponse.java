package com.smartdrive.userservice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for token claims response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenClaimsResponse {

    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles;
    private Boolean isEnabled;
    private Boolean isEmailVerified;
}
