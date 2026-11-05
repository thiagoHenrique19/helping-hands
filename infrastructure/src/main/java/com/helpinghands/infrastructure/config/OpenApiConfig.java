package com.helpinghands.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI helpingHandsOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Helping Hands API")
                .description("REST API connecting NGOs, volunteers and donors. Errors follow RFC 7807 "
                        + "(application/problem+json) and every response carries an X-Request-Id header.")
                .version("v0.2.0")
                .contact(new Contact().name("Helping Hands").url("https://github.com/thiagoHenrique19/helping-hands")));
    }
}
