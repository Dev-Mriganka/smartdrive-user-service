package com.smartdrive.userservice.service;

import com.smartdrive.userservice.model.Role;
import com.smartdrive.userservice.model.Role.RoleType;
import com.smartdrive.userservice.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Service to initialize default roles in the database
 * Runs on application startup to ensure required roles exist
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleInitializationService implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("🚀 Initializing default roles for User Service...");
        initializeDefaultRoles();
        log.info("✅ Default roles initialization completed");
    }

    /**
     * Initialize default roles if they don't exist
     */
    private void initializeDefaultRoles() {
        List<RoleType> defaultRoles = Arrays.asList(
            RoleType.SMARTDRIVE_USER,
            RoleType.SMARTDRIVE_ADMIN,
            RoleType.SMARTDRIVE_VIEWER,
            RoleType.SMARTDRIVE_GUEST
        );

        for (RoleType roleType : defaultRoles) {
            if (!roleRepository.existsByName(roleType)) {
                Role role = Role.builder()
                    .name(roleType)
                    .description(roleType.getDescription())
                    .isActive(true)
                    .build();
                
                Role savedRole = roleRepository.save(role);
                log.info("✅ Created default role: {}", savedRole.getName());
            } else {
                log.info("📋 Role already exists: {}", roleType);
            }
        }
    }
}
