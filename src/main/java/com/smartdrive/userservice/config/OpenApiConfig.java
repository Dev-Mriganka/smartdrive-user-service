package com.smartdrive.userservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for User Service
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SmartDrive User Service API")
                        .description("""
                                User Management Service for SmartDrive Platform
                                
                                This service provides comprehensive user management capabilities including:
                                - User registration and profile management
                                - Email verification and account activation
                                - Password management and security features
                                - Two-factor authentication support
                                - Role-based access control
                                - Admin functions for user management
                                
                                ## Authentication
                                This service is designed to work with the SmartDrive API Gateway.
                                All requests should be routed through the gateway with proper JWT authentication.
                                
                                ## Database
                                Uses dedicated PostgreSQL database: `smartdrive_users`
                                
                                ## Security
                                - OAuth2 Resource Server (JWT validation)
                                - Rate limiting and security headers
                                - Input validation and sanitization
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SmartDrive Team")
                                .email("support@smartdrive.com")
                                .url("https://smartdrive.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8086")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.smartdrive.com/user-service")
                                .description("Production Server")
                ));
    }
}
