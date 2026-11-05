package com.helpinghands.infrastructure.security;

import static org.mockito.Mockito.mock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Brings the production security rules into {@code @WebMvcTest} slices. Token decoding and ownership
 * checks are replaced by mocks (ownership checks deny by default).
 */
@TestConfiguration
@Import({SecurityConfig.class, ProblemDetailSecurityHandler.class})
public class WebMvcSecurityTestConfig {

    @Bean
    JwtDecoder jwtDecoder() {
        return mock(JwtDecoder.class);
    }

    @Bean("access")
    AccessPolicy accessPolicy() {
        return mock(AccessPolicy.class);
    }
}
