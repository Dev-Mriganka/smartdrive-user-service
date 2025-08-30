package com.smartdrive.userservice.service;

import com.smartdrive.userservice.model.Role;
import com.smartdrive.userservice.model.Role.RoleType;
import com.smartdrive.userservice.model.User;
import com.smartdrive.userservice.repository.RoleRepository;
import com.smartdrive.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * Service to initialize default admin user in the database
 * Runs after role initialization to ensure required admin user exists
 * 
 * Security Note: In production, the admin password should be:
 * 1. Generated randomly and stored securely
 * 2. Set via environment variables or secure vault
 * 3. Forced to change on first login
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Order(2) // Run after RoleInitializationService
public class AdminUserInitializationService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("🚀 Initializing default admin user for User Service...");
        initializeDefaultAdminUser();
        log.info("✅ Default admin user initialization completed");
    }

    /**
     * Initialize default admin user if it doesn't exist
     */
    private void initializeDefaultAdminUser() {
        String adminUsername = "admin";
        String adminEmail = "admin@smartdrive.com";
        
        // Check if admin user already exists
        if (userRepository.existsByUsername(adminUsername)) {
            log.info("📋 Admin user already exists: {}", adminUsername);
            
            // Ensure admin has correct roles
            User existingAdmin = userRepository.findByUsername(adminUsername).get();
            ensureAdminHasCorrectRoles(existingAdmin);
            return;
        }

        // Get admin role
        Role adminRole = roleRepository.findByName(RoleType.SMARTDRIVE_ADMIN)
                .orElseThrow(() -> new RuntimeException("Admin role not found - ensure RoleInitializationService ran first"));

        // Get user role (admin should also have basic user permissions)
        Role userRole = roleRepository.findByName(RoleType.SMARTDRIVE_USER)
                .orElseThrow(() -> new RuntimeException("User role not found"));

        // Create admin user
        User adminUser = User.builder()
                .username(adminUsername)
                .email(adminEmail)
                .password(passwordEncoder.encode("Admin123!")) // TODO: Use environment variable in production
                .firstName("System")
                .lastName("Administrator")
                .bio("Default system administrator account")
                .isEnabled(true)
                .isEmailVerified(true) // Admin doesn't need email verification
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        // Set admin roles
        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        roles.add(userRole);
        adminUser.setRoles(roles);

        User savedAdmin = userRepository.save(adminUser);

        log.info("✅ Created default admin user: {} with roles: {}", 
                savedAdmin.getUsername(), 
                savedAdmin.getRoleNames());
        log.warn("⚠️  SECURITY WARNING: Default admin password is 'Admin123!' - Change this in production!");
    }

    /**
     * Ensure existing admin user has correct roles
     */
    private void ensureAdminHasCorrectRoles(User adminUser) {
        boolean hasAdminRole = adminUser.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleType.SMARTDRIVE_ADMIN);
        
        if (!hasAdminRole) {
            log.info("🔧 Adding admin role to existing admin user: {}", adminUser.getUsername());
            
            Role adminRole = roleRepository.findByName(RoleType.SMARTDRIVE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));
            
            Set<Role> roles = new HashSet<>(adminUser.getRoles());
            roles.add(adminRole);
            adminUser.setRoles(roles);
            
            userRepository.save(adminUser);
            
            log.info("✅ Admin role added to user: {}", adminUser.getUsername());
        }
    }
}
