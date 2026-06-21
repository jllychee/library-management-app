package com.ilhanozkan.libraryManagementSystem.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    Server server = new Server()
        .url("http://localhost:8080/api/v1")
        .description("REST API - Development");

    Contact contact = new Contact()
        .name("Library Management System")
        .email("contact.ilhanozkan@gmail.com");

    License license = new License()
        .name("Apache 2.0")
        .url("http://www.apache.org/licenses/LICENSE-2.0");

    Info info = new Info()
        .title("Library Management System")
        .version("1.0")
        .contact(contact)
        .description("This is the library Management System REST API")
        .license(license);

    SecurityScheme bearerScheme = new SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT");

    // HTTP Basic scheme so Swagger's Authorize dialog accepts username/password
    // directly (e.g. librarian/password) instead of forcing token copy-paste.
    SecurityScheme basicScheme = new SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("basic");

    return new OpenAPI()
        .info(info)
        .servers(List.of(server))
        // basicAuth is the default lock on each endpoint; bearerAuth stays available.
        .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
        .components(new Components()
            .addSecuritySchemes("bearerAuth", bearerScheme)
            .addSecuritySchemes("basicAuth", basicScheme));
  }
}
