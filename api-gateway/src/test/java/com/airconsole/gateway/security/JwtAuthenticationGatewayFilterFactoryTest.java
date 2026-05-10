package com.airconsole.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class JwtAuthenticationGatewayFilterFactoryTest {

    private JwtAuthenticationGatewayFilterFactory filterFactory;
    private final String secret = "this-is-a-test-secret-key-that-must-be-long-enough-32bytes-min!!";

    @BeforeEach
    void setUp() {
        filterFactory = new JwtAuthenticationGatewayFilterFactory(secret);
    }

    @Test
    void shouldRejectWhenNoAuthHeader() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test").build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        GatewayFilter filter = filterFactory.apply(new JwtAuthenticationGatewayFilterFactory.Config());
        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldAcceptValidTokenAndAddHeaders() {
        UUID playerId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        String role = "HOST";

        String token = Jwts.builder()
            .subject(playerId.toString())
            .claim("playerId", playerId.toString())
            .claim("roomId", roomId.toString())
            .claim("role", role)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
            .compact();

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        GatewayFilter filter = filterFactory.apply(new JwtAuthenticationGatewayFilterFactory.Config());
        filter.filter(exchange, chain).block();

        assertThat(exchange.getRequest().getHeaders().getFirst("X-Player-Id")).isEqualTo(playerId.toString());
        assertThat(exchange.getRequest().getHeaders().getFirst("X-Room-Id")).isEqualTo(roomId.toString());
        assertThat(exchange.getRequest().getHeaders().getFirst("X-Role")).isEqualTo(role);
    }
}
