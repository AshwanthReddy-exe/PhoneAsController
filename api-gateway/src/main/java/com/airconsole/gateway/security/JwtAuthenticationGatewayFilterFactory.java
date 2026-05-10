package com.airconsole.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JwtAuthenticationGatewayFilterFactory 
        extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {

    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticationGatewayFilterFactory.class);
    private final SecretKey key;

    public JwtAuthenticationGatewayFilterFactory(
            @Value("${jwt.secret:default-very-long-secret-key-for-development-only}") String secret) {
        super(Config.class);
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                LOG.warn("Missing or invalid Authorization header");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

                // Add headers for downstream services
                exchange.getRequest().mutate()
                    .header("X-Player-Id", claims.get("playerId", String.class))
                    .header("X-Room-Id", claims.get("roomId", String.class))
                    .header("X-Role", claims.get("role", String.class))
                    .build();

            } catch (Exception e) {
                LOG.warn("Invalid JWT token: {}", e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            return chain.filter(exchange);
        };
    }

    public static class Config {
        // Configuration properties can go here if needed
    }
}
