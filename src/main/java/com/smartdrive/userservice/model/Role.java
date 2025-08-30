package com.smartdrive.userservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Role entity for user authorization
 */
@Entity
@Table(name = "roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "users")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false, length = 20)
    private RoleType name;

    @Column(length = 255)
    private String description;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @ManyToMany(mappedBy = "roles")
    @Builder.Default
    @JsonBackReference
    private Set<User> users = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        // Default behavior on creation if needed
    }

    @PreUpdate
    protected void onUpdate() {
        // Default behavior on update if needed
    }

    /**
     * Role types for SmartDrive application
     */
    public enum RoleType {
        SMARTDRIVE_USER("Regular user with basic access"),
        SMARTDRIVE_ADMIN("Administrator with full access"),
        SMARTDRIVE_VIEWER("Read-only user"),
        SMARTDRIVE_SUPPORT("Support staff with limited admin access"),
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
