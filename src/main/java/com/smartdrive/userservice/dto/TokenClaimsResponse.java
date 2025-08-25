package com.smartdrive.userservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TokenClaimsResponse {
    
    private Long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles;
    private Boolean isEnabled;
    private Boolean isEmailVerified;
}
