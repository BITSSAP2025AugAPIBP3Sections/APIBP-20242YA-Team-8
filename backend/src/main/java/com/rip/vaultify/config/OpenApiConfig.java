package com.rip.vaultify.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Vaultify API",
                version = "v1.0",
                description = """
                        REST API for Vaultify - A modern file storage and sharing application.
                        
                        ## Features
                        - User authentication with JWT
                        - File upload, download, and management
                        - Folder organization
                        - File sharing with granular permissions (READ/WRITE/OWNER)
                        - Pre-signed URLs for secure file access
                        - Idempotency support for reliable operations
                        - ETag-based caching
                        
                        ## Authentication
                        Most endpoints require JWT authentication. Use the `/auth/login` endpoint to obtain a token,
                        then include it in the Authorization header as: `Bearer <your-token>`
                        
                        ## Rate Limiting
                        - Authentication endpoints: 5 requests per 60 seconds
                        - General API endpoints: 100 requests per 60 seconds
                        - File upload endpoints: 30 requests per 60 seconds
                        """,
                contact = @Contact(
                        name = "Vaultify Support",
                        email = "support@vaultify.com"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(
                        url = "http://localhost:8080",
                        description = "Local Development Server"
                )
        },
        security = {@SecurityRequirement(name = "bearerAuth")}
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT Authentication. Obtain a token from /auth/login endpoint."
)
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("vaultify-api")
                .pathsToMatch("/**")
                .build();
    }
}


