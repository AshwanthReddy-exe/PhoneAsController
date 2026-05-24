package com.airconsole.player.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.airconsole.player.domain.JwtIssuer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtIssuerTest {

    String secret = "test-secret-key-must-be-at-least-256-bits-long-for-security-reasons-abc123";
    JwtIssuer issuer = new JwtIssuer(secret);

    @Test
    void issuesValidToken() {
        UUID playerId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        String token = issuer.issue(playerId, roomId, "HOST");

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    void tokenContainsCorrectClaims() {
        UUID playerId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        String token = issuer.issue(playerId, roomId, "GUEST");

        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(secret.getBytes()))
            .build()
            .parseSignedClaims(token)
            .getPayload();

        assertThat(claims.get("playerId", String.class)).isEqualTo(playerId.toString());
        assertThat(claims.get("roomId", String.class)).isEqualTo(roomId.toString());
        assertThat(claims.get("role", String.class)).isEqualTo("GUEST");
        assertThat(claims.getExpiration()).isNotNull();
    }
}
