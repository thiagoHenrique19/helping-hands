package com.helpinghands.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.helpinghands.core.domain.Role;
import com.helpinghands.core.domain.User;
import com.helpinghands.core.port.out.TokenIssuer.IssuedToken;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenIssuerTest {

    private static final String SECRET = "unit-test-secret-with-more-than-32-bytes!!";

    private final JwtConfig config = new JwtConfig();

    private JwtTokenIssuer issuerAt(Instant now, JwtProperties properties) {
        var key = config.jwtSecretKey(properties);
        return new JwtTokenIssuer(config.jwtEncoder(key), properties, Clock.fixed(now, ZoneOffset.UTC));
    }

    private User user() {
        User user = User.register("ngo@mail.com", "hash", Role.ORGANIZATION);
        ReflectionTestUtils.setField(user, "id", 15L);
        return user;
    }

    @Test
    void shouldIssueDecodableTokenWithUserClaims() {
        var properties = new JwtProperties(SECRET, Duration.ofMinutes(30));
        IssuedToken token = issuerAt(Instant.now(), properties).issue(user());
        JwtDecoder decoder = config.jwtDecoder(config.jwtSecretKey(properties));

        Jwt jwt = decoder.decode(token.value());

        assertThat(jwt.getSubject()).isEqualTo("15");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ORGANIZATION");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("ngo@mail.com");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("helping-hands");
        assertThat(token.expiresInSeconds()).isEqualTo(1800);
    }

    @Test
    void shouldIssueTokenRejectedAfterExpiry() {
        var properties = new JwtProperties(SECRET, Duration.ofMinutes(5));
        IssuedToken token = issuerAt(Instant.now().minus(Duration.ofHours(2)), properties).issue(user());

        assertThatThrownBy(() -> config.jwtDecoder(config.jwtSecretKey(properties)).decode(token.value()))
                .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void shouldRejectTokenSignedWithAnotherSecret() {
        var properties = new JwtProperties(SECRET, Duration.ofMinutes(5));
        var other = new JwtProperties("another-secret-with-more-than-32-bytes!!!!", Duration.ofMinutes(5));
        IssuedToken token = issuerAt(Instant.now(), other).issue(user());

        assertThatThrownBy(() -> config.jwtDecoder(config.jwtSecretKey(properties)).decode(token.value()))
                .isInstanceOf(BadJwtException.class);
    }

    @Test
    void shouldRejectWeakSecrets() {
        assertThatThrownBy(() -> new JwtProperties("too-short", Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtProperties(null, Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldHashPasswordsWithBCrypt() {
        var hasher = new BCryptPasswordHasher(config.passwordEncoder());

        String hash = hasher.hash("secret-pass");

        assertThat(hash).startsWith("$2").isNotEqualTo("secret-pass");
        assertThat(hasher.matches("secret-pass", hash)).isTrue();
        assertThat(hasher.matches("other-pass", hash)).isFalse();
    }
}
