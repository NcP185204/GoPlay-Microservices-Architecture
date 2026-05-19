package com.caophuc.court.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Court Microservice API")
                        .version("1.0")
                        .description("API documentation for Court Service in GoPlay System")
                        .contact(new Contact().name("Cao Phuc").email("test@example.com")));
    }
}
