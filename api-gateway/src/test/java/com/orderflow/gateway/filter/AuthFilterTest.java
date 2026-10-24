package com.orderflow.gateway.filter;

import com.orderflow.gateway.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AuthFilterTest {

    private static final String SECRET = "test-secret-key-for-unit-tests-only-256-bits-minimum-length-padding-pad";

    private AuthFilter filter;

    @BeforeEach
    void setup() {
        filter = new AuthFilter(new JwtUtil(SECRET));
    }

    private SecretKey key(String s) {
        return Keys.hmacShaKeyFor(s.getBytes(StandardCharsets.UTF_8));
    }

    private String token(String name, String role, long expMs) {
        return token(name, role, false, expMs);
    }

    private String token(String name, String role, boolean superAdmin, long expMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject("cesar@email.com")
                .claim("name", name)
                .claim("role", role)
                .claim("superAdmin", superAdmin)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expMs))
                .signWith(key(SECRET), Jwts.SIG.HS256)
                .compact();
    }

    private record CapturingChain(AtomicReference<ServerWebExchange> captured) implements GatewayFilterChain {
        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            captured.set(exchange);
            return Mono.empty();
        }
    }

    @Test
    void publicRoute_passesThroughAndStripsForgedHeaders() {
        var captured = new AtomicReference<ServerWebExchange>();
        MockServerHttpRequest req = MockServerHttpRequest.post("/auth/login")
                .header("X-User-Name", "ATTACKER")
                .header("X-User-Role", "ADMIN")
                .header("X-User-Super-Admin", "true")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);

        filter.filter(exchange, new CapturingChain(captured)).block();

        ServerHttpRequest forwarded = captured.get().getRequest();
        assertThat(forwarded.getHeaders().get("X-User-Name")).isNull();
        assertThat(forwarded.getHeaders().get("X-User-Role")).isNull();
        assertThat(forwarded.getHeaders().get("X-User-Super-Admin")).isNull();
    }

    @Test
    void authUsersRoute_isProtected_returns401WithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/auth/users").build());

        filter.filter(exchange, new CapturingChain(new AtomicReference<>())).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedRoute_missingAuthorization_returns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/orders").build());

        filter.filter(exchange, new CapturingChain(new AtomicReference<>())).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedRoute_nonBearerScheme_returns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Basic abcd")
                        .build());

        filter.filter(exchange, new CapturingChain(new AtomicReference<>())).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedRoute_expiredToken_returns401() throws InterruptedException {
        String t = token("César", "USER", 1L);
        Thread.sleep(20);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + t)
                        .build());

        filter.filter(exchange, new CapturingChain(new AtomicReference<>())).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedRoute_validToken_injectsHeadersAndStripsClientHeaders() {
        var captured = new AtomicReference<ServerWebExchange>();
        String t = token("César", "USER", 86400000L);
        MockServerHttpRequest req = MockServerHttpRequest.get("/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + t)
                .header("X-User-Name", "ATTACKER")
                .header("X-User-Role", "ADMIN")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);

        filter.filter(exchange, new CapturingChain(captured)).block();

        ServerHttpRequest forwarded = captured.get().getRequest();
        assertThat(forwarded.getHeaders().getFirst("X-User-Name")).isEqualTo("César");
        assertThat(forwarded.getHeaders().getFirst("X-User-Role")).isEqualTo("USER");
        assertThat(forwarded.getHeaders().getFirst("X-User-Email")).isEqualTo("cesar@email.com");
        assertThat(forwarded.getHeaders().getFirst("X-User-Super-Admin")).isEqualTo("false");
        assertThat(forwarded.getHeaders().get("X-User-Name")).hasSize(1);
        assertThat(forwarded.getHeaders().get("X-User-Role")).hasSize(1);
        assertThat(forwarded.getHeaders().get("X-User-Email")).hasSize(1);
        assertThat(forwarded.getHeaders().get("X-User-Super-Admin")).hasSize(1);
    }

    @Test
    void protectedRoute_superAdminToken_injectsTrue() {
        var captured = new AtomicReference<ServerWebExchange>();
        String t = token("Admin", "ADMIN", true, 86400000L);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + t)
                        .build());

        filter.filter(exchange, new CapturingChain(captured)).block();

        assertThat(captured.get().getRequest().getHeaders().getFirst("X-User-Super-Admin")).isEqualTo("true");
    }

    @Test
    void protectedRoute_tamperedToken_returns401() {
        String t = token("César", "USER", 86400000L);
        String tampered = t.substring(0, t.length() - 4) + "AAAA";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered)
                        .build());

        filter.filter(exchange, new CapturingChain(new AtomicReference<>())).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
