package com.helpinghands.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.security.jwt")
public record JwtProperties(String secret, Duration ttl) {

    public static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "app.security.jwt.secret (JWT_SECRET) must be at least " + MIN_SECRET_BYTES + " bytes long");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            ttl = Duration.ofHours(1);
        }
    }
}
