package com.airconsole.player.domain;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;

public final class JwtIssuer {

    private final SecretKey key;

    public JwtIssuer(byte[] secret) {
        this.key = Keys.hmacShaKeyFor(secret);
    }

    public JwtIssuer(String secret) {
        this(secret.getBytes());
    }

    public String issue(UUID playerId, UUID roomId, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(24, ChronoUnit.HOURS);

        return Jwts.builder()
            .subject(playerId.toString())
            .claim("playerId", playerId.toString())
            .claim("roomId", roomId.toString())
            .claim("role", role)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(key)
            .compact();
    }
}
