package com.kshitij.tms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger Configuration
 * Generates interactive API documentation at: http://localhost:8080/swagger-ui/index.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tmsOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("Development Server");

        Contact contact = new Contact();
        contact.setEmail("careers@cargopro.ai");
        contact.setName("Kshitij");

        Info info = new Info()
                .title("Transport Management System API")
                .version("1.0.0")
                .contact(contact)
                .description("RESTful API for managing loads, transporters, bids, and bookings in a logistics platform. "
                        + "Implements complex business rules including capacity validation, status transitions, "
                        + "multi-truck allocation, and concurrent booking prevention with optimistic locking.");

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}