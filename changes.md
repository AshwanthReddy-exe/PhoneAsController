# changes.md

# AirConsole Clone — Architecture Improvements & Future Enhancements

This document lists architectural improvements identified after reviewing the initial system design. The goal is to keep the project modular, scalable, and technically impressive while avoiding unnecessary complexity.

---

# 1. Replace Redis Pub/Sub with Redis Streams for Critical Events

## Current

* All inter-service communication uses Redis Pub/Sub.

## Problem

Redis Pub/Sub is ephemeral. If a consumer service is temporarily down, messages are lost forever.

Example:

* `game.finished` event is published.
* `score-service` is offline.
* Final scores are never persisted.

## Proposed Change

Use:

* **Redis Streams** for business-critical events.
* Optionally keep Pub/Sub only for transient real-time updates.

### Critical Events

* room.created
* room.expired
* player.joined
* player.disconnected
* player.rejoined
* player.left
* game.started
* game.finished

### Optional Real-Time Events

* game.state.updated

## Benefits

* Event durability.
* Replay capability.
* Consumer groups.
* Easier migration path to Kafka later.

---

# 2. Move Game Inputs from REST to WebSockets

## Current

```
Phone
   ↓
REST
   ↓
API Gateway
   ↓
Game Service
```

## Problem

REST adds unnecessary latency for real-time games like Pong or Snake.

## Proposed

```
Phone
   ↓
Azure SignalR
   ↓
Notification Service
   ↓
Game Service Input Queue
```

* Controllers send inputs via WebSocket.
* Game state is broadcast via WebSocket.
* REST endpoints remain available only for testing/debugging.

## Benefits

* Lower latency.
* Symmetric communication model.
* More realistic multiplayer architecture.

---

# 3. Introduce a Game Engine Plugin System

## Current

Game logic is likely to be selected using conditional logic or switch statements.

## Proposed

Create a common interface:

```java
public interface GameEngine {
    GameType getType();
    void initialize(GameContext context);
    void processInput(GameInput input);
    void tick();
    GameSnapshot snapshot();
    boolean isFinished();
}
```

Implementations:

* SnakeGame
* PongGame
* TriviaGame
* Future games...

A registry maps `GameType -> GameEngine`.

## Benefits

* Open/Closed Principle.
* Adding new games requires minimal code changes.
* Easier testing and maintenance.

---

# 4. Separate Game Engine Components Internally

## Current

Game Service owns:

* game lifecycle
* game loop
* input processing
* state management
* individual game logic

## Proposed Structure

```
game-service/
├── api/
├── engine/
│   ├── InputQueue.java
│   ├── GameLoopScheduler.java
│   ├── GameStateManager.java
│   └── EngineRegistry.java
├── games/
│   ├── GameEngine.java
│   ├── SnakeGame.java
│   ├── PongGame.java
│   └── TriviaGame.java
└── infrastructure/
```

## Benefits

* Cleaner separation of concerns.
* Better long-term maintainability.
* Simpler addition of future games.

---

# 5. Add a Game Catalog

## Current

Available games are hardcoded inside `GameType` enum.

## Proposed

Maintain a `GameCatalog` table:

| Field            | Description              |
| ---------------- | ------------------------ |
| gameId           | Unique identifier        |
| name             | Display name             |
| minPlayers       | Minimum players          |
| maxPlayers       | Maximum players          |
| controllerLayout | D-Pad / Buttons / Custom |
| tickRate         | Engine tick speed        |
| enabled          | Enable/Disable game      |

Frontend loads available games dynamically.

## Benefits

* No hardcoded game list.
* Easy to enable/disable games.
* Foundation for community-created games.

---

# 6. Improve Room Code Generation

## Current

* 4 uppercase letters.
* Total combinations: 26^4 = 456,976.

## Proposed

Use 5-character Crockford Base32 codes.

Examples:

* 7KXQH
* AB9PD
* X7M4R

## Benefits

* Over 33 million combinations.
* Avoids visually confusing characters.
* Reduces collision probability.

---

# 7. Add Idempotency Protection

The following endpoints should safely handle retries:

* POST /api/rooms/create
* POST /api/players/reconnect
* POST /api/games/start

## Implementation

* RequestId header.
* Store processed request IDs in Redis with short TTL.
* Duplicate requests return original response.

## Benefits

* Handles mobile network retries.
* Prevents duplicate game sessions.

---

# 8. Add Invite Tokens

## Current

Anyone knowing a room code can repeatedly attempt registration.

## Proposed

Room creation generates a temporary invite token.

Example:

```
https://play.example.com/join?room=XKQP&invite=abc123
```

Player registration validates:

* room code
* invite token

## Benefits

* Prevents room enumeration.
* More secure join flow.
* QR codes naturally carry the token.

---

# 9. Add Spectator Mode

Support users joining as viewers without controller permissions.

```
Room
├── Screen
├── Controllers
└── Spectators
```

Spectators:

* Receive game state updates.
* Cannot send inputs.

## Benefits

* Better multiplayer experience.
* Nice feature during demos.
* Very low implementation cost.

---

# 10. Add Replay System

Store every processed input event:

```
InputEvent
------------
timestamp
tickNumber
playerId
action
```

A replay engine rebuilds game state by replaying events.

## Benefits

* Match replay functionality.
* Deterministic debugging.
* Strong portfolio feature.

---

# 11. Add Analytics & Metrics

Expose additional Prometheus metrics:

* Total rooms created.
* Active rooms.
* Average game duration.
* Inputs processed per second.
* Reconnect success rate.
* Most played game.
* Player abandonment rate.

Display through Grafana dashboards.

## Benefits

* Better observability.
* Demonstrates production engineering practices.

---

# 12. Improve Frontend Monorepo Structure

## Current

```
frontend/
├── lobby/
├── screen/
└── controller/
```

## Proposed

```
frontend/
├── apps/
│   ├── lobby/
│   ├── screen/
│   └── controller/
├── shared/
│   ├── api/
│   ├── websocket/
│   ├── qr/
│   └── ui/
```

## Benefits

* Shared utilities.
* Less code duplication.
* Cleaner scaling.

---

# 13. Simplify Initial Deployment

## Current

* Oracle VM 1
* Oracle VM 2

## Proposed (Phase 1)

Run everything on a single Oracle VM using Docker Compose.

```
Nginx
Gateway
Room Service
Player Service
Game Service
Notification Service
Score Service
Prometheus
Grafana
```

Move to multi-VM deployment only if resource usage demands it.

## Benefits

* Easier operations.
* Simpler debugging.
* Lower deployment complexity.

---

# 14. Add an Explicit Game Plugin Contract

Document the contract for future game developers.

```java
public interface GameEngine {
    GameType getType();
    void initialize(GameContext context);
    void processInput(GameInput input);
    void tick();
    GameSnapshot snapshot();
    boolean isFinished();
}
```

### To Add a New Game

1. Implement `GameEngine`.
2. Register the engine.
3. Add frontend assets.
4. Add an entry in the Game Catalog.

No existing engine code should require modification.

---

# 15. Future Stretch Goal: Custom Game SDK

Long-term vision:
Allow third-party developers to build games against a lightweight SDK.

Example:

```java
public interface AirConsoleGame {
    void onStart(GameContext context);
    void onInput(PlayerInput input);
    void onTick();
    GameState render();
}
```

The platform becomes not only an AirConsole clone, but a lightweight browser-based multiplayer game hosting platform.

---

# 16. Keep / Remove Decisions

## Keep

* Server-side authoritative game loop.
* Event-driven microservice architecture.
* Azure SignalR for state broadcasting.
* Redis for game state storage.
* Neon PostgreSQL for persistent data.
* 60-second soft reconnect window.
* API Gateway with JWT validation.
* Prometheus + Grafana monitoring.

## Reconsider

* Redis Pub/Sub for all events.
* REST-only controller inputs.
* Hardcoded game list.
* 4-character room codes.
* Multi-VM deployment from day one.
* Denormalized `playerCount` without documenting eventual consistency.

---

# Final Vision

The target architecture should evolve from:

> "An AirConsole clone built with Spring Boot microservices."

to

> "A modular multiplayer browser gaming platform with pluggable game engines, event-driven architecture, real-time communication, replay capability, and a future path toward third-party game hosting."

