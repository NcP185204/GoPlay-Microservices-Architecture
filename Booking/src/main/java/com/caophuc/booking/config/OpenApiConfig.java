package com.caophuc.booking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bookingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Booking Service API")
                        .description("Tài liệu API cho Microservice Đặt Sân (Booking)")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Cao Phuc")
                                .email("phuc@example.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8083").description("Local Server")
                ));
    }
}