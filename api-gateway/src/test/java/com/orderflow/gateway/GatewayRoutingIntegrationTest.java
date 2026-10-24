package com.orderflow.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingIntegrationTest {

    private static final String SECRET = "test-secret-key-for-unit-tests-only-256-bits-minimum-length-padding-pad";

    static WireMockServer authStub;
    static WireMockServer orderStub;

    @BeforeAll
    static void startStubs() {
        authStub = new WireMockServer(options().port(18181));
        orderStub = new WireMockServer(options().port(18282));
        authStub.start();
        orderStub.start();
    }

    @AfterAll
    static void stopStubs() {
        authStub.stop();
        orderStub.stop();
    }

    @BeforeEach
    void resetStubs() {
        authStub.resetAll();
        orderStub.resetAll();
    }

    @DynamicPropertySource
    static void overrideRoutes(DynamicPropertyRegistry r) {
        r.add("spring.cloud.gateway.routes[0].id", () -> "auth-service");
        r.add("spring.cloud.gateway.routes[0].uri", () -> "http://localhost:18181");
        r.add("spring.cloud.gateway.routes[0].predicates[0]", () -> "Path=/auth/**");
        r.add("spring.cloud.gateway.routes[1].id", () -> "order-service");
        r.add("spring.cloud.gateway.routes[1].uri", () -> "http://localhost:18282");
        r.add("spring.cloud.gateway.routes[1].predicates[0]", () -> "Path=/orders/**");
        r.add("jwt.secret", () -> SECRET);
    }

    @Autowired WebTestClient client;

    private String validToken() {
        Date now = new Date();
        SecretKey k = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("cesar@email.com")
                .claim("name", "César")
                .claim("role", "USER")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 86400000L))
                .signWith(k, Jwts.SIG.HS256)
                .compact();
    }

    @Test
    void publicRoute_routesToAuthServiceWithoutToken() {
        authStub.stubFor(post(urlEqualTo("/auth/login"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"token\":\"abc\"}")));

        client.post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"x\",\"password\":\"y\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.token").isEqualTo("abc");
    }

    @Test
    void protectedRoute_noToken_returns401() {
        client.get().uri("/orders").exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void protectedRoute_validToken_forwardsWithIdentityHeaders() {
        orderStub.stubFor(get(urlEqualTo("/orders"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        client.get().uri("/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken())
                .header("X-User-Name", "ATTACKER")
                .header("X-User-Role", "ADMIN")
                .exchange()
                .expectStatus().isOk();

        orderStub.verify(getRequestedFor(urlEqualTo("/orders"))
                .withHeader("X-User-Name", equalTo("César"))
                .withHeader("X-User-Role", equalTo("USER")));
    }

    @Test
    void protectedRoute_invalidToken_returns401WithJsonBody() {
        client.get().uri("/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody().jsonPath("$.message").isEqualTo("Token inválido");
    }
}
