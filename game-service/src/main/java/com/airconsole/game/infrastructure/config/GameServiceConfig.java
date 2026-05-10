package com.airconsole.game.infrastructure.config;

import com.airconsole.common.engine.GameEngine;
import com.airconsole.game.application.GameEventPublisher;
import com.airconsole.game.domain.GameOrchestrator;
import com.airconsole.game.domain.GameSessionRepository;
import com.airconsole.game.domain.engine.EngineRegistry;
import com.airconsole.game.domain.engine.GameLoopScheduler;
import com.airconsole.game.domain.engine.GameStateManager;
import java.util.Optional;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class GameServiceConfig {

    private static final Logger LOG = LoggerFactory.getLogger(GameServiceConfig.class);

    @Bean
    public EngineRegistry engineRegistry() {
        EngineRegistry registry = new EngineRegistry();

        // Classpath scanning via ServiceLoader for GameEngine implementations
        ServiceLoader<GameEngine> loader = ServiceLoader.load(GameEngine.class);
        for (GameEngine engine : loader) {
            LOG.info("Registering game engine: {}", engine.getType());
            registry.register(engine);
        }

        // Fallback: if no SPI found, look for known engines in games/ modules
        registerKnownEngines(registry);

        return registry;
    }

    @Bean
    public GameLoopScheduler gameLoopScheduler(EngineRegistry registry, GameEventPublisher eventPublisher) {
        return new GameLoopScheduler(registry, eventPublisher);
    }

    @Bean
    public GameStateManager gameStateManager(StringRedisTemplate redisTemplate) {
        return new GameStateManager(redisTemplate);
    }

    @Bean
    public GameOrchestrator gameOrchestrator(
            GameSessionRepository repository,
            EngineRegistry registry,
            GameLoopScheduler scheduler,
            GameStateManager stateManager,
            GameEventPublisher eventPublisher) {
        return new GameOrchestrator(repository, registry, scheduler, stateManager, eventPublisher);
    }

    private void registerKnownEngines(EngineRegistry registry) {
        // Try to instantiate known game engines via reflection
        try {
            GameEngine snakeEngine = (GameEngine) Class
                .forName("com.airconsole.games.snake.engine.SnakeGame")
                .getDeclaredConstructor()
                .newInstance();
            registry.register(snakeEngine);
            LOG.info("Registered Snake game engine via reflection");
        } catch (Exception ex) {
            LOG.warn("Snake game engine not available on classpath: {}", ex.getMessage());
        }

        try {
            GameEngine pongEngine = (GameEngine) Class
                .forName("com.airconsole.games.pong.engine.PongGame")
                .getDeclaredConstructor()
                .newInstance();
            registry.register(pongEngine);
            LOG.info("Registered Pong game engine via reflection");
        } catch (Exception ex) {
            LOG.warn("Pong game engine not available on classpath: {}", ex.getMessage());
        }

        try {
            GameEngine triviaEngine = (GameEngine) Class
                .forName("com.airconsole.games.trivia.engine.TriviaGame")
                .getDeclaredConstructor()
                .newInstance();
            registry.register(triviaEngine);
            LOG.info("Registered Trivia game engine via reflection");
        } catch (Exception ex) {
            LOG.warn("Trivia game engine not available on classpath: {}", ex.getMessage());
        }
    }
}
