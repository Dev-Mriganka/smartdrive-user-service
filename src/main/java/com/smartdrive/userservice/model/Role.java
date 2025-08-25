package com.smartdrive.userservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Role entity for user authorization
 */
@Entity
@Table(name = "roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private RoleType name;

    private String description;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Role types for SmartDrive application
     */
    public enum RoleType {
        SMARTDRIVE_USER("Regular user with basic access"),
        SMARTDRIVE_ADMIN("Administrator with full access"),
        SMARTDRIVE_VIEWER("Read-only user"),
        SMARTDRIVE_GUEST("Guest user with limited access");

        private final String description;

        RoleType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
