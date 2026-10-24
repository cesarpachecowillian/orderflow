package com.orderflow.auth.service;

import com.orderflow.auth.entity.Role;
import com.orderflow.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-for-unit-tests-only-256-bits-minimum-length-padding-pad";

    private User user() {
        var u = new User("César", "cesar@email.com", "x", Role.USER);
        u.setId(1L);
        return u;
    }

    @Test
    void generate_and_parse_returnsClaims() {
        var jwt = new JwtService(SECRET, 86400000L);
        String token = jwt.generate(user());

        Claims c = jwt.parse(token);
        assertThat(c.getSubject()).isEqualTo("cesar@email.com");
        assertThat(c.get("name", String.class)).isEqualTo("César");
        assertThat(c.get("role", String.class)).isEqualTo("USER");
        assertThat(c.get("superAdmin", Boolean.class)).isFalse();
        assertThat(c.getExpiration()).isAfter(c.getIssuedAt());
    }

    @Test
    void generate_includesSuperAdminClaimWhenSet() {
        var jwt = new JwtService(SECRET, 86400000L);
        var u = user();
        u.setSuperAdmin(true);
        Claims c = jwt.parse(jwt.generate(u));
        assertThat(c.get("superAdmin", Boolean.class)).isTrue();
    }

    @Test
    void extractHelpers() {
        var jwt = new JwtService(SECRET, 86400000L);
        String token = jwt.generate(user());

        assertThat(jwt.extractEmail(token)).isEqualTo("cesar@email.com");
        assertThat(jwt.extractName(token)).isEqualTo("César");
        assertThat(jwt.extractRole(token)).isEqualTo(Role.USER);
    }

    @Test
    void expiredToken_throws() throws InterruptedException {
        var jwt = new JwtService(SECRET, 1L);
        String token = jwt.generate(user());
        Thread.sleep(20);
        assertThatThrownBy(() -> jwt.parse(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tamperedToken_throws() {
        var jwt = new JwtService(SECRET, 86400000L);
        String token = jwt.generate(user());
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThatThrownBy(() -> jwt.parse(tampered))
                .isInstanceOfAny(SignatureException.class, io.jsonwebtoken.MalformedJwtException.class);
    }

    @Test
    void wrongSecret_throws() {
        var producer = new JwtService(SECRET, 86400000L);
        var consumer = new JwtService("different-secret-key-256-bits-minimum-length-padding-padding-padding-pad", 86400000L);
        String token = producer.generate(user());
        assertThatThrownBy(() -> consumer.parse(token))
                .isInstanceOf(SignatureException.class);
    }
}
