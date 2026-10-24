package com.orderflow.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {

    public enum Outcome { VALID, EXPIRED, INVALID }

    public record Result(Outcome outcome, Claims claims) {
        public static Result valid(Claims c) { return new Result(Outcome.VALID, c); }
        public static Result expired() { return new Result(Outcome.EXPIRED, null); }
        public static Result invalid() { return new Result(Outcome.INVALID, null); }
    }

    private final SecretKey signingKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Result verify(String token) {
        if (token == null || token.isBlank()) return Result.invalid();
        try {
            Claims c = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Result.valid(c);
        } catch (ExpiredJwtException ex) {
            return Result.expired();
        } catch (Exception ex) {
            return Result.invalid();
        }
    }
}
