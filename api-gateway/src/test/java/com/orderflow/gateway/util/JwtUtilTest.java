package com.orderflow.gateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-for-unit-tests-only-256-bits-minimum-length-padding-pad";
    private final JwtUtil util = new JwtUtil(SECRET);

    private SecretKey key(String s) {
        return Keys.hmacShaKeyFor(s.getBytes(StandardCharsets.UTF_8));
    }

    private String build(String secret, String name, String role, long expiresInMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject("cesar@email.com")
                .claim("name", name)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiresInMs))
                .signWith(key(secret), Jwts.SIG.HS256)
                .compact();
    }

    @Test
    void validToken() {
        String token = build(SECRET, "César", "USER", 86400000L);
        JwtUtil.Result r = util.verify(token);
        assertThat(r.outcome()).isEqualTo(JwtUtil.Outcome.VALID);
        assertThat(r.claims().getSubject()).isEqualTo("cesar@email.com");
        assertThat(r.claims().get("name", String.class)).isEqualTo("César");
        assertThat(r.claims().get("role", String.class)).isEqualTo("USER");
    }

    @Test
    void expiredToken() throws InterruptedException {
        String token = build(SECRET, "César", "USER", 1L);
        Thread.sleep(20);
        assertThat(util.verify(token).outcome()).isEqualTo(JwtUtil.Outcome.EXPIRED);
    }

    @Test
    void wrongSignature() {
        String token = build("different-secret-key-256-bits-minimum-length-padding-padding-padding-pad", "X", "USER", 86400000L);
        assertThat(util.verify(token).outcome()).isEqualTo(JwtUtil.Outcome.INVALID);
    }

    @Test
    void malformed() {
        assertThat(util.verify("not.a.token").outcome()).isEqualTo(JwtUtil.Outcome.INVALID);
        assertThat(util.verify("").outcome()).isEqualTo(JwtUtil.Outcome.INVALID);
        assertThat(util.verify(null).outcome()).isEqualTo(JwtUtil.Outcome.INVALID);
    }
}
