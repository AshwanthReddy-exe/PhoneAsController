package com.airconsole.game.domain.engine;

import com.airconsole.common.model.GameSnapshot;
import com.airconsole.common.util.EventSerializer;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Persists/loads game state to Redis (optional — primarily in-memory).
 * Flush on checkpoint (every 10 ticks or player event).
 */
public class GameStateManager {

    private static final Logger LOG = LoggerFactory.getLogger(GameStateManager.class);
    private static final String KEY_PREFIX = "game:state:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;

    public GameStateManager(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void checkpoint(UUID gameId, GameSnapshot snapshot) {
        try {
            String json = EventSerializer.getMapper().writeValueAsString(snapshot);
            redisTemplate.opsForValue().set(KEY_PREFIX + gameId, json, TTL);
            LOG.debug("Checkpoint game {} at tick {}", gameId, snapshot.getTickNumber());
        } catch (Exception ex) {
            LOG.warn("Checkpoint failed for {}", gameId, ex);
        }
    }

    public GameSnapshot load(UUID gameId) {
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + gameId);
            if (json != null) {
                return EventSerializer.getMapper().readValue(json, GameSnapshot.class);
            }
        } catch (Exception ex) {
            LOG.warn("Load failed for {}", gameId, ex);
        }
        return null;
    }

    public void delete(UUID gameId) {
        redisTemplate.delete(KEY_PREFIX + gameId);
    }
}
