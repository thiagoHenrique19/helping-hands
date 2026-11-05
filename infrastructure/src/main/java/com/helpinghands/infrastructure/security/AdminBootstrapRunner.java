package com.helpinghands.infrastructure.security;

import com.helpinghands.core.port.in.BootstrapAdminUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final BootstrapAdminUseCase bootstrapAdmin;
    private final String email;
    private final String password;

    AdminBootstrapRunner(BootstrapAdminUseCase bootstrapAdmin,
                         @Value("${app.admin.email:}") String email,
                         @Value("${app.admin.password:}") String password) {
        this.bootstrapAdmin = bootstrapAdmin;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        if (bootstrapAdmin.bootstrapAdmin(email, password)) {
            log.info("Bootstrap administrator {} created", email);
        }
    }
}
