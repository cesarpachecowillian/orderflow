package com.orderflow.gateway.filter;

import com.orderflow.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class AuthFilter implements GlobalFilter, Ordered {

    static final String HEADER_USER_NAME = "X-User-Name";
    static final String HEADER_USER_ROLE = "X-User-Role";
    static final String HEADER_USER_EMAIL = "X-User-Email";
    static final String HEADER_USER_SUPER_ADMIN = "X-User-Super-Admin";
    static final String BEARER_PREFIX = "Bearer ";

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/auth/login",
            "/auth/register",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars/",
            "/actuator/health"
    );

    private final JwtUtil jwtUtil;

    public AuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublic(path)) {
            return chain.filter(stripIdentityHeaders(exchange));
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || authHeader.isBlank()) {
            return unauthorized(exchange, "Token não fornecido");
        }
        if (!authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange, "Token inválido");
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        JwtUtil.Result result = jwtUtil.verify(token);
        return switch (result.outcome()) {
            case EXPIRED -> unauthorized(exchange, "Token expirado");
            case INVALID -> unauthorized(exchange, "Token inválido");
            case VALID -> chain.filter(injectIdentity(exchange, result.claims()));
        };
    }

    private boolean isPublic(String path) {
        for (String p : PUBLIC_PREFIXES) {
            if (path.startsWith(p)) return true;
        }
        return false;
    }

    private ServerWebExchange stripIdentityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove(HEADER_USER_NAME);
                    h.remove(HEADER_USER_ROLE);
                    h.remove(HEADER_USER_EMAIL);
                    h.remove(HEADER_USER_SUPER_ADMIN);
                })
                .build();
        return exchange.mutate().request(mutated).build();
    }

    private ServerWebExchange injectIdentity(ServerWebExchange exchange, Claims claims) {
        String name = claims.get("name", String.class);
        String role = claims.get("role", String.class);
        String email = claims.getSubject();
        Boolean superAdmin = claims.get("superAdmin", Boolean.class);
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove(HEADER_USER_NAME);
                    h.remove(HEADER_USER_ROLE);
                    h.remove(HEADER_USER_EMAIL);
                    h.remove(HEADER_USER_SUPER_ADMIN);
                    if (name != null) h.add(HEADER_USER_NAME, name);
                    if (role != null) h.add(HEADER_USER_ROLE, role);
                    if (email != null) h.add(HEADER_USER_EMAIL, email);
                    h.add(HEADER_USER_SUPER_ADMIN, Boolean.TRUE.equals(superAdmin) ? "true" : "false");
                })
                .build();
        return exchange.mutate().request(mutated).build();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + escape(message) + "\"}";
        DataBuffer buf = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buf));
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
