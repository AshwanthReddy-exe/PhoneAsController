package com.airconsole.game.domain.engine;

import com.airconsole.common.enums.GameType;
import com.airconsole.common.engine.GameEngine;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps GameType → GameEngine at runtime.
 * Service scans games/* JARs at startup.
 * Lock-free reads (ConcurrentHashMap).
 */
public final class EngineRegistry {

    private final Map<GameType, GameEngine> engines = new ConcurrentHashMap<>();

    public void register(GameEngine engine) {
        GameType type = engine.getType();
        GameEngine existing = engines.putIfAbsent(type, engine);
        if (existing != null) {
            throw new IllegalStateException("Engine for " + type + " already registered");
        }
    }

    public GameEngine get(GameType type) {
        GameEngine engine = engines.get(type);
        if (engine == null) {
            throw new IllegalArgumentException("No engine registered for game type: " + type);
        }
        return engine;
    }

    public boolean isRegistered(GameType type) {
        return engines.containsKey(type);
    }

    public int size() {
        return engines.size();
    }
}
