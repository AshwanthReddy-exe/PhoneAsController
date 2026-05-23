package com.airconsole.player.infrastructure.config;

import com.airconsole.player.application.PlayerEventPublisher;
import com.airconsole.player.domain.JwtIssuer;
import com.airconsole.player.domain.PlayerRepository;
import com.airconsole.player.domain.PlayerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlayerServiceConfig {

    @Value("${jwt.secret:default-very-long-secret-key-for-development-only}")
    private String jwtSecret;

    @Bean
    public JwtIssuer jwtIssuer() {
        return new JwtIssuer(jwtSecret);
    }

    @Bean
    public PlayerService playerService(
            PlayerRepository repository,
            PlayerEventPublisher eventPublisher,
            JwtIssuer jwtIssuer) {
        return new PlayerService(repository, eventPublisher, jwtIssuer);
    }
}
