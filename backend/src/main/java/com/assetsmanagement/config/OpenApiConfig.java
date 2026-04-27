package com.assetsmanagement.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger documentation configuration.
 * Enables Bearer JWT authentication in Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI assetsManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Assets Management API")
                        .description("REST API for the Assets Management System. "
                                + "Use the Authorize button to set your JWT Bearer token.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Assets Management Team")))
                .components(new Components()
                        .addSecuritySchemes("Bearer", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT access token")));
    }
}
