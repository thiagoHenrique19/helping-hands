package com.helpinghands;

import com.helpinghands.core.domain.Role;
import com.helpinghands.core.domain.User;
import com.helpinghands.core.port.out.PasswordHasher;
import com.helpinghands.core.port.out.TokenIssuer;
import com.helpinghands.core.port.out.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractPostgresIntegrationTest {

    private static final String ADMIN_EMAIL = "integration-admin@test.org";

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired protected MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordHasher passwordHasher;
    @Autowired private TokenIssuer tokenIssuer;

    protected RequestPostProcessor admin() {
        User admin = userRepository.findByEmail(ADMIN_EMAIL)
                .orElseGet(() -> userRepository.save(
                        User.register(ADMIN_EMAIL, passwordHasher.hash("admin-password"), Role.ADMIN)));
        return bearer(tokenIssuer.issue(admin).value());
    }

    protected RequestPostProcessor bearer(String token) {
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return request;
        };
    }
}
