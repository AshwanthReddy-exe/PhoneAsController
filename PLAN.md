# AirConsole Clone — Modular Phase-Wise Execution Plan

> This plan enforces **extreme modularity** — every service is independently buildable, independently testable, and independently runnable.
>
> **Rule**: Never mix concerns. Never code ahead of phase. Never skip tests.
>
> **Resume**: You can start at any phase boundary because each phase produces deployable, tested artifacts.

---

## Phase 0: Foundation & Toolchain
### Goal: Immutable project skeleton. Zero business logic.

**Files to create:**
```
airconsole/
├── settings.gradle               ← root settings, includes all modules (.gradle = Groovy DSL)
├── build.gradle                  ← root build script (BOM, conventions)
├── buildSrc/
│   ├── build.gradle              ← buildSrc builds itself
│   └── src/main/groovy/
│       ├── java-conventions.gradle           ← shared Java setup
│       ├── spring-boot-conventions.gradle    ← Spring Boot services
│       ├── testing-conventions.gradle        ← JUnit, Mockito, Testcontainers
│       └── publishing-conventions.gradle     ← local Maven publishing
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew / gradlew.bat
├── .gitignore
├── README.md                     ← project overview + run instructions
├── .editorconfig                 ← consistent formatting
├── Makefile                      ← one-command operations
└── infra/
    ├── docker-compose.yml        ← local dev stack (DB, Redis, Observability)
    ├── docker-compose.prod.yml   ← production stack
    └── Makefile                  ← infra operations
```

**Root `build.gradle` must enforce:**
- Java 21 toolchain
- Spring Boot 3.3 BOM via `io.spring.dependency-management`
- `buildSrc` convention plugins applied to modules
- Lombok (annotation processor)
- MapStruct (annotation processor)
- JUnit 5 + Mockito + AssertJ (via `testing-conventions`)
- Testcontainers BOM
- JaCoCo (code coverage) — min 70% on `domain` packages
- Checkstyle plugin
- Gradle Wrapper
- Modules: `common-lib`, `room-service`, `player-service`, `game-service`, `notification-service`, `score-service`, `api-gateway`, `frontend`, `games/*`

**Local Dev Stack (`infra/docker-compose.yml`):**
- PostgreSQL (persistent data)
- Redis (Streams + Pub/Sub + Game State)
- Prometheus (metrics)
- Grafana (dashboards)
- Loki (logs)
- Jaeger (traces)

**Acceptance Criteria:**
- [ ] `make dev` starts PostgreSQL + Redis + all observability tools.
- [ ] `make test` runs all tests via `./gradlew test`.
- [ ] `make build` builds all modules via `./gradlew build`.
- [ ] `make clean` removes everything via `./gradlew clean`.
- [ ] Git initialized with `main` branch.

---

## Phase 1: `common-lib` — Shared Contracts (Independent)
### Goal: Define the language all services speak.

**Structure:**
```
common-lib/
├── pom.xml
└── src/main/java/com/airconsole/common/
    ├── dto/
    │   ├── RoomDTO.java
    │   ├── PlayerDTO.java
    │   └── GameInputDTO.java
    ├── events/
    │   ├── RoomCreatedEvent.java
    │   ├── RoomExpiredEvent.java
    │   ├── PlayerJoinedEvent.java
    │   ├── PlayerDisconnectedEvent.java
    │   ├── PlayerRejoinedEvent.java
    │   ├── PlayerLeftEvent.java
    │   ├── GameStartedEvent.java
    │   ├── GameStateUpdatedEvent.java
    │   ├── GamePausedEvent.java
    │   ├── GameResumedEvent.java
    │   └── GameFinishedEvent.java
    ├── enums/
    │   ├── RoomStatus.java       → WAITING, PLAYING, FINISHED, EXPIRED
    │   ├── PlayerRole.java       → HOST, GUEST
    │   ├── PlayerStatus.java     → CONNECTED, DISCONNECTED, RECONNECTING
    │   ├── GameType.java         → SNAKE (PONG, TRIVIA added later)
    │   └── GameStatus.java       → WAITING, RUNNING, PAUSED, FINISHED
    ├── model/
    │   ├── GameInput.java        ← normalized controller input
    │   ├── GameSnapshot.java     ← serialized game state
    │   └── GameContext.java      ← roomId, players, config
    ├── exceptions/
    │   ├── AirConsoleException.java (base)
    │   ├── RoomNotFoundException.java
    │   ├── PlayerNotFoundException.java
    │   ├── GameNotFoundException.java
    │   ├── InvalidInputException.java
    │   └── ConflictException.java
    ├── util/
    │   ├── RoomCodeGenerator.java (Crockford Base32, 5 chars)
    │   ├── JwtUtil.java
    │   ├── IdempotencyKeyUtil.java
    │   └── ValidationUtil.java
    └── messaging/
        ├── EventEnvelope.java      ← wraps every event with metadata (id, timestamp, source, version)
        ├── EventSerializer.java    ← JSON ser/de
        └── ChannelNames.java       ← constants for Redis channels/streams
```

**Key Decisions:**
- Pure Java. **No Spring Boot dependency**. Only `jackson-annotations`.
- All events are immutable records (or Lombok `@Value`).
- `EventEnvelope` guarantees traceability and idempotency.
- `RoomCodeGenerator`: Crockford Base32, 5 chars = 33M combinations, no ambiguous chars.
- **Latency Budget**: Every event is designed for sub-millisecond serialization. JSON pool reuses `ObjectMapper` via static thread-local. Avoid `Map.of()` and complex generics in hot paths.

**Acceptance Criteria:**
- [ ] 100% unit test coverage on `util/` and `events/`.
- [ ] `./gradlew :common-lib:publishToMavenLocal` publishes to local Maven.
- [ ] No Spring annotations anywhere in this module.

---

## Phase 2: `room-service` — Room Lifecycle (Port 8081)
### Goal: Create, manage, and expire game rooms.
- Depends on: `common-lib`
- Storage: PostgreSQL
- Events: Publishes `RoomCreatedEvent`, `RoomExpiredEvent`. Consumes `PlayerJoinedEvent`, `PlayerLeftEvent`.

**Architecture (Hexagonal):**
```
room-service/
├── src/main/java/com/airconsole/room/
│   ├── RoomServiceApplication.java
│   ├── api/
│   │   ├── RoomController.java
│   │   ├── GlobalExceptionHandler.java
│   │   ├── request/
│   │   │   ├── CreateRoomRequest.java
│   │   │   └── JoinRoomRequest.java
│   │   └── response/
│   │       ├── RoomResponse.java
│   │       └── RoomListResponse.java
│   ├── domain/
│   │   ├── Room.java                    ← pure logic, no annotations
│   │   ├── RoomCode.java                ← value object
│   │   ├── RoomStatus.java              ← enum
│   │   ├── RoomRepository.java          ← interface (port)
│   │   ├── RoomService.java             ← domain orchestrator
│   │   └── RoomExpiryScheduler.java    ← background cleaner
│   ├── application/
│   │   ├── RoomEventPublisher.java      ← application port
│   │   ├── RoomEventHandler.java        ← listens to player events
│   │   └── dto/
│   │       └── RoomUpdateCommand.java
│   └── infrastructure/
│       ├── config/
│       │   ├── RedisConfig.java
│       │   └── JacksonConfig.java
│       ├── persistence/
│       │   ├── RoomJpaEntity.java       ← JPA entity
│       │   ├── RoomJpaRepository.java   ← Spring Data
│       │   └── RoomRepositoryAdapter.java ← adapter (hexagonal)
│       └── messaging/
│           ├── RedisRoomEventPublisher.java
│           └── RedisPlayerEventListener.java
└── src/test/java/com/airconsole/room/
    ├── unit/
    │   ├── domain/RoomTest.java
    │   ├── domain/RoomCodeTest.java
    │   └── domain/RoomServiceTest.java
    └── integration/
        ├── RoomControllerIT.java         ← Testcontainers (PostgreSQL + Redis)
        └── RoomExpirySchedulerIT.java
```

**API Contract:**
```
POST /api/rooms          → CreateRoomRequest  → RoomResponse (201)
GET  /api/rooms/{code}   → code               → RoomResponse (200, 404)
DELETE /api/rooms/{id}   → id + JWT host     → 204 (403 if not host)
GET  /api/rooms/{code}/status → code        → RoomStatusResponse
```

**Event Flow:**
- On `POST /api/rooms`: Domain creates `Room` → `RoomRepositoryAdapter` persists → `RedisRoomEventPublisher` emits `RoomCreatedEvent` (Redis Stream).
- `RoomExpiryScheduler` scans expired rooms every 60s → emits `RoomExpiredEvent` → deletes room.
- `RedisPlayerEventListener` consumes `PlayerJoinedEvent` / `PlayerLeftEvent` → updates `playerCount`.

**Acceptance Criteria:**
- [ ] Full unit test suite for `domain/` (pure logic, no mocks needed).
- [ ] Integration tests with Testcontainers.
- [ ] Room expires correctly after 30m of inactivity.
- [ ] Crockford Base32 codes are unique.
- [ ] Player count updates via events (no direct service calls).
- [ ] Code coverage ≥ 70%.

---

## Phase 3: `player-service` — Identity & Reconnection (Port 8082)
### Goal: Register players, manage sessions, handle reconnects.
- Depends on: `common-lib`
- Storage: PostgreSQL
- Auth: Issues JWTs (sub = playerId, roomId, role).
- Events: Publishes `PlayerJoinedEvent`, `PlayerDisconnectedEvent`, `PlayerRejoinedEvent`, `PlayerLeftEvent`. Consumes `RoomExpiredEvent`.

**Architecture (Hexagonal):**
```
player-service/
├── src/main/java/com/airconsole/player/
│   ├── PlayerServiceApplication.java
│   ├── api/
│   │   ├── PlayerController.java
│   │   ├── GlobalExceptionHandler.java
│   │   ├── request/
│   │   │   ├── RegisterPlayerRequest.java
│   │   │   └── ReconnectPlayerRequest.java
│   │   └── response/
│   │       ├── PlayerResponse.java
│   │       └── TokenResponse.java
│   ├── domain/
│   │   ├── Player.java
│   │   ├── PlayerRole.java
│   │   ├── PlayerStatus.java
│   │   ├── PlayerRepository.java      ← port
│   │   ├── PlayerService.java
│   │   ├── RejoinWindowTracker.java   ← domain logic for 60s window
│   │   └── JwtIssuer.java             ← domain auth (no Spring Security yet)
│   ├── application/
│   │   ├── PlayerEventPublisher.java  ← port
│   │   ├── PlayerEventHandler.java    ← room events
│   │   └── dto/
│   └── infrastructure/
│       ├── config/
│       ├── persistence/
│       │   ├── PlayerJpaEntity.java
│       │   ├── PlayerJpaRepository.java
│       │   └── PlayerRepositoryAdapter.java
│       └── messaging/
│           ├── RedisPlayerEventPublisher.java
│           └── RedisRoomEventListener.java
└── src/test/
    ├── unit/...
    └── integration/
        └── PlayerControllerIT.java
```

**API Contract:**
```
POST /api/players/register    → RegisterPlayerRequest → TokenResponse (201)
POST /api/players/reconnect   → ReconnectPlayerRequest → TokenResponse (200)
GET  /api/players/{roomId}    → roomId + JWT           → PlayerListResponse
PATCH /api/players/{id}/connection  → connectionId    → 200 (called by Notification Service)
```

**Rejoin Logic (Domain, not infra):**
1. Disconnect: `PlayerStatus → DISCONNECTED`, `lastSeenAt = now()`.
2. `RejoinWindowTracker` publishes `PlayerLeftEvent` after 60s if not rejoined.
3. Reconnect: check `lastSeenAt` < 60s → `PlayerStatus → CONNECTED` → publish `PlayerRejoinedEvent`.

**Acceptance Criteria:**
- [ ] JWTs issued correctly (suite of unit tests).
- [ ] Rejoin window works: disconnected → rejoined = OK; disconnected > 60s = marked left.
- [ ] `RoomExpiredEvent` removes all players for that room.
- [ ] Full integration tests with Testcontainers.
- [ ] No direct calls to `room-service`. All communication is event-driven.

---

## Phase 4: `game-service` — Plugin Engine & Snake (Port 8083)
### Goal: Authoritative game loop with pluggable engines — one JAR per game.
- Depends on: `common-lib`
- Storage: Redis (game state — speed > persistence)
- Events: Publishes `GameStartedEvent`, `GameStateUpdatedEvent`, `GamePausedEvent`, `GameResumedEvent`, `GameFinishedEvent`. Consumes `PlayerJoinedEvent`, `PlayerDisconnectedEvent`, `PlayerRejoinedEvent`, `PlayerLeftEvent`, `RoomExpiredEvent`.

**Key Latency Requirement**: Input → broadcast must be < **50ms end-to-end** (optimally < 20ms). Every design choice below serves this budget.

**Architecture (Hexagonal + Hyper-Modular Plugin System):**
```
game-service/
├── src/main/java/com/airconsole/game/
│   ├── GameServiceApplication.java
│   ├── api/
│   │   ├── GameController.java           ← REST only for admin/debug
│   │   └── GameAdminController.java
│   ├── domain/
│   │   ├── engine/
│   │   │   ├── GameEngine.java           ← pure interface (contract)
│   │   │   ├── EngineRegistry.java       ← maps GameType → GameEngine (loaded from classpath)
│   │   │   ├── GameLoopScheduler.java    ← ticks every N ms (dedicated thread per active room)
│   │   │   ├── RingBufferInputQueue.java ← lock-free MPSC ring buffer per room (JCTools or Agrona)
│   │   │   ├── GameStateManager.java     ← loads/saves state to Redis (binary protobuf, not JSON)
│   │   │   ├── GameContext.java          ← room config + players
│   │   │   ├── GameInput.java            ← normalized action (int opcode, int payload)
│   │   │   └── GameSnapshot.java         ← serializable state (protobuf)
│   │   ├── controller/
│   │   │   └── ControllerLayout.java     ← defines buttons, axes, d-pad per game
│   │   ├── session/
│   │   │   ├── GameSession.java          ← aggregate root
│   │   │   ├── ActiveSessionPool.java    ← lock-free concurrent map of roomId → session
│   │   │   └── SessionMetrics.java       ← tick latency histogram
│   │   ├── GameSessionRepository.java    ← port
│   │   └── GameOrchestrator.java         ← starts, stops, pauses games
│   ├── application/
│   │   ├── GameEventPublisher.java
│   │   ├── GameEventHandler.java        ← player/room events
│   │   └── WebSocketInputHandler.java   ← processes inputs from Notification Service
│   └── infrastructure/
│       ├── config/
│       ├── cache/
│       │   ├── GameStateRedisEntity.java
│       │   ├── GameStateRedisRepository.java
│       │   └── GameStateRedisAdapter.java  ← protobuf ser/de, not JSON
│       └── messaging/
│           ├── RedisGameEventPublisher.java  ← binary pub/sub for state updates (not JSON)
│           ├── RedisPlayerEventListener.java
│           └── RedisRoomEventListener.java
```

**Games as Independent Gradle Submodules:**
Each game is a **separate Gradle module** producing its own JAR. Games do NOT depend on `game-service`. `game-service` depends on game JARs at runtime via classpath scanning.
```
games/
├── snake/
│   ├── build.gradle.kts
│   └── src/main/java/com/airconsole/games/snake/
│       ├── SnakeGame.java          ← implements GameEngine
│       ├── SnakeState.java
│       ├── SnakePlayer.java
│       ├── SnakeCollisionChecker.java
│       └── SnakeControllerLayout.java  ← defines D-PAD
├── pong/
│   ├── build.gradle.kts
│   └── src/main/java/com/airconsole/games/pong/
│       ├── PongGame.java
│       ├── PongControllerLayout.java   ← defines LEFT / RIGHT buttons
│       └── ...                        
└── trivia/
    ├── build.gradle.kts
    └── src/main/java/com/airconsole/games/trivia/
        ├── TriviaGame.java
        ├── TriviaControllerLayout.java ← defines A/B/C/D buttons
        └── ...
```

**Game Engine Contract (Pure Interface — Zero Spring):**
```java
public interface GameEngine {
    GameType getType();
    ControllerLayout getControllerLayout();  ← tells frontend what buttons to render
    void initialize(GameContext context);
    void processInput(GameInput input);
    void tick();                              ← deterministic, no I/O
    GameSnapshot snapshot();                  ← binary protobuf, compact
    boolean isFinished();
}
```

**ControllerLayout Contract:**
```java
public interface ControllerLayout {
    String getGameType();
    List<Button> getButtons();     ← label, actionCode, position (x,y,w,h)
    boolean hasDPad();
    boolean hasJoystick();
    // Future: haptic patterns, custom CSS class hints
}
```

**Game Loop (Latency-Optimized):**
1. **Dedicated thread per active room** — `GameLoopScheduler` uses `ScheduledThreadPoolExecutor`.
2. **Lock-free input queue** — `RingBufferInputQueue` (MPSC, one producer per player).
3. **Tick pipeline**: drain queue → `processInput()` (0-allocation) → `tick()` → `snapshot()` → publish `GameStateUpdatedEvent`.
4. **Binary broadcast** — `GameSnapshot` serialized to protobuf (not JSON). Feeds Redis Pub/Sub in binary.
5. **No Redis write on every tick** — state kept in-memory; flushed to Redis every N ticks or on `GameSnapshot` checkpoints.

**Latency Budget (Target < 50ms E2E):**
| Stage | Budget | Technique |
|-------|--------|-----------|
| Controller press → SignalR | < 10ms | Azure SignalR ping |
| SignalR → Notification Svc | < 5ms | WebSocket in same AZ |
| Notification → Game Svc | < 5ms | Redis Pub/Sub (binary) |
| Game Svc input queue | < 2ms | Lock-free ring buffer |
| Game tick + snapshot | < 5ms | 0-allocation game logic |
| Game Svc → Redis broadcast | < 5ms | Binary protobuf Pub/Sub |
| Redis → Notification Svc | < 5ms | Sub-microsecond local |
| Notification → SignalR | < 5ms | REST call (compressed) |
| SignalR → Screen | < 10ms | WebSocket |
| **Total** | **~ 47ms** | **All stages measured via Micrometer timers** |

**Acceptance Criteria:**
- [ ] `SnakeGame` unit tests: movement, collision, scoring, winning.
- [ ] `EngineRegistry` auto-discovers `GameEngine` implementations from `games/*` on classpath.
- [ ] Game pauses on `PlayerDisconnectedEvent`, resumes on `PlayerRejoinedEvent`.
- [ ] **Controller layout is served dynamically** per game type via `GET /api/games/{type}/layout`.
- [ ] Latency metrics exported: `game.tick.duration`, `game.input_queue.wait`, `game.e2e.latency`.
- [ ] Lock-free input queue benchmarked under 10k inputs/sec.
- [ ] Full integration tests with Testcontainers.

---

## Phase 5: `notification-service` — SignalR Bridge (Port 8084)
### Goal: Bridge internal events to external WebSocket clients.
- Depends on: `common-lib`
- Storage: None (stateless)
- Events: Consumes ALL events from all services. Publishes nothing.

**Architecture:**
```
notification-service/
├── src/main/java/com/airconsole/notification/
│   ├── NotificationServiceApplication.java
│   ├── api/
│   │   └── SignalRWebhookController.java  ← Azure SignalR webhooks
│   ├── domain/
│   │   ├── EventRouter.java               ← routes events to SignalR groups
│   │   ├── GroupMapper.java               ← roomCode → SignalR group
│   │   └── ConnectionTracker.java         ← playerId ↔ connectionId
│   ├── application/
│   │   ├── SignalRPublisher.java          ← port
│   │   └── PlayerConnectionUpdater.java   ← calls Player Service on connect/disconnect
│   ├── infrastructure/
│   │   ├── signalr/
│   │   │   ├── AzureSignalRPublisher.java     ← calls Azure SignalR REST API
│   │   │   └── SignalRConfig.java
│   │   └── messaging/
│   │       └── RedisEventConsumer.java        ← subscribes to ALL events
│   └── dto/
│       └── WebSocketMessage.java
```

**Flow:**
1. `RedisEventConsumer` listens to all Redis Streams.
2. `EventRouter` maps `roomId` → `roomCode` → SignalR group.
3. `AzureSignalRPublisher` broadcasts to group.
4. On SignalR connect/disconnect webhook: `PlayerConnectionUpdater` calls `PATCH /api/players/{id}/connection`.

**Acceptance Criteria:**
- [ ] All events routed to correct SignalR group.
- [ ] Connect/disconnect webhooks update Player Service correctly.
- [ ] Unit tests mock SignalR REST calls.

---

## Phase 6: `score-service` — Leaderboards (Port 8085)
### Goal: Persist final scores, serve leaderboard/match history.
- Depends on: `common-lib`
- Storage: PostgreSQL
- Events: Consumes `GameFinishedEvent`. Publishes nothing.

**Architecture:**
```
score-service/
├── src/main/java/com/airconsole/score/
│   ├── ScoreServiceApplication.java
│   ├── api/
│   │   ├── LeaderboardController.java
│   │   └── HistoryController.java
│   ├── domain/
│   │   ├── Score.java
│   │   ├── ScoreRepository.java
│   │   └── LeaderboardService.java
│   ├── application/
│   │   └── GameFinishedEventHandler.java
│   └── infrastructure/
│       ├── persistence/
│       │   ├── ScoreJpaEntity.java
│       │   ├── ScoreJpaRepository.java
│       │   └── ScoreRepositoryAdapter.java
│       └── messaging/
│           └── RedisGameFinishedListener.java
```

**API Contract:**
```
GET /api/leaderboard/{gameType}?limit=10 → LeaderboardResponse
GET /api/history/{playerId}              → MatchHistoryResponse
```

**Acceptance Criteria:**
- [ ] Scores persisted on `GameFinishedEvent`.
- [ ] Leaderboard queries are performant (indexed on `gameType`, `score`).
- [ ] Integration tests verify event consumption.

---

## Phase 7: `api-gateway` — Single Entry Point (Port 8080)
### Goal: Route, validate, protect. No business logic.
- Depends on: `common-lib` (for JWT validation)
- Storage: Redis (rate limiting, JWT blacklist)

**Responsibilities:**
- Route all `/api/**` traffic to correct service.
- Validate JWT on every request (except `/api/rooms/create`, `/api/rooms/join`, `/api/players/register`).
- Rate limit per IP (Redis-backed).
- Inject `X-Player-Id`, `X-Room-Id`, `X-Role` headers into downstream requests.
- CORS configuration.

**Route Table:**
```
POST /api/rooms          → room-service:8081    (no auth)
GET  /api/rooms/**       → room-service:8081    (JWT)
POST /api/players/register → player-service:8082 (no auth)
GET  /api/players/**     → player-service:8082  (JWT)
POST /api/games/**       → game-service:8083    (JWT + HOST check for /start)
GET  /api/leaderboard/** → score-service:8085   (no auth)
GET  /api/history/**     → score-service:8085   (JWT)
```

**Acceptance Criteria:**
- [ ] JWT validation works (unit tests).
- [ ] Rate limiting enforced (Redis-backed bucket algorithm).
- [ ] Correct headers injected.
- [ ] Integration tests verify routing.

---

## Phase 8: `frontend/` — React + Vite Monorepo
### Goal: Lobby, Screen, Controller UIs built as a modern React monorepo.
- Tech: **React 19 + Vite + TypeScript** for all apps.
- State: **Zustand** for global state (player, room, game).
- Real-time: **Azure SignalR** via `@microsoft/signalr`.
- Styling: **Tailwind CSS** + **shadcn/ui** components.
- Build: **Vite** with module federation (or import maps) for game-specific controller layouts.
- Hosting: Static files served by Nginx (built via `vite build`).

**Monorepo Structure (using pnpm workspaces):**
```
frontend/
├── package.json              ← pnpm workspaces root
├── pnpm-workspace.yaml       ← apps/* + packages/*
├── turbo.json                ← build pipeline
├── apps/
│   ├── lobby/
│   │   ├── index.html
│   │   ├── src/
│   │   │   ├── main.tsx
│   │   │   ├── App.tsx
│   │   │   ├── pages/
│   │   │   │   ├── HomePage.tsx          ← create room / join room
│   │   │   │   └── JoinPage.tsx          ← enter room code / scan QR
│   │   │   └── hooks/
│   │   │       └── useLobbyApi.ts
│   │   └── vite.config.ts
│   ├── screen/
│   │   ├── index.html
│   │   ├── src/
│   │   │   ├── main.tsx
│   │   │   ├── App.tsx
│   │   │   ├── pages/
│   │   │   │   └── GameScreen.tsx        ← SignalR group + canvas/game renderers
│   │   │   ├── renderers/
│   │   │   │   ├── SnakeRenderer.tsx     ← React component + Canvas
│   │   │   │   ├── PongRenderer.tsx
│   │   │   │   └── TriviaRenderer.tsx
│   │   │   └── hooks/
│   │   │       ├── useSignalR.ts         ← connection + group join
│   │   │       └── useGameState.ts       ← Zustand store for realtime state
│   │   └── vite.config.ts
│   └── controller/
│       ├── index.html
│       ├── src/
│       │   ├── main.tsx
│       │   ├── App.tsx
│       │   ├── pages/
│       │   │   └── ControllerPage.tsx    ← dynamic layout from GameEngine
│       │   ├── layouts/
│       │   │   ├── DPadLayout.tsx        ← Snake
│       │   │   ├── TwoButtonLayout.tsx   ← Pong
│       │   │   └── ABCDLayout.tsx        ← Trivia
│       │   ├── hooks/
│       │   │   ├── useSignalR.ts
│       │   │   └── useControllerInput.ts ← debounced 16ms input dispatch
│       │   └── components/
│       │       └── DynamicController.tsx ← fetches layout from API, renders the right component
│       └── vite.config.ts
└── packages/
    ├── shared/
    │   ├── src/
    │   │   ├── api/
    │   │   │   └── client.ts              ← fetch + Zod runtime validation
    │   │   ├── signalr/
    │   │   │   └── connection.ts          ← shared SignalR manager
    │   │   ├── types/
    │   │   │   ├── game.ts                ← GameType, GameInput, GameSnapshot
    │   │   │   └── controller.ts          ← ControllerLayout types
    │   │   └── ui/
    │   │       └── Button.tsx             ← shared shadcn button
    │   └── package.json
    └── tailwind-config/
        └── tailwind.config.ts            ← shared Tailwind preset
```

**Dynamic Controller Layout Flow:**
1. Controller app joins room via SignalR.
2. On `game.started` event, backend sends `gameType`.
3. Frontend calls `GET /api/games/{gameType}/layout` → receives JSON `ControllerLayout`.
4. `DynamicController.tsx` switches on `layout.gameType` → renders matching layout component.
5. **Third-party games**: Drop a new layout component in `controller/layouts/` + register in `DynamicController.tsx`. No backend changes needed if game JAR is already deployed.

**Latency Considerations on Frontend:**
- Controller input dispatch is **debounced at 16ms** (one frame at 60fps), not per keystroke.
- Screen receives state updates via SignalR `onMessage` handler → updates Zustand store → triggers React re-render of canvas.
- **Canvas rendering** for games uses `requestAnimationFrame` + `useRef` (not React virtual DOM) to avoid GC pauses during gameplay.

**Acceptance Criteria:**
- [ ] Lobby creates/joins rooms (React SPA with React Router).
- [ ] Screen renders game state in real-time via SignalR + Canvas.
- [ ] Controller **fetches layout dynamically** from backend per game type.
- [ ] Controller supports D-Pad, Two-Button, and A/B/C/D layouts.
- [ ] Adding a new game requires: backend game JAR + frontend renderer + controller layout. **No existing service code changes.**
- [ ] All three apps are separate Vite builds, sharing code via `packages/shared`.
- [ ] Static assets served by Nginx with gzip/brotli compression.

---

## Phase 9: Observability Stack
### Goal: Full visibility into the system.

**Components:**
| Tool | Purpose |
|------|---------|
| **Prometheus** | Metrics: `rooms_created_total`, `players_connected`, `game_ticks_per_second`, `api_latency_seconds` |
| **Grafana** | Dashboards: Service health, game metrics, Redis throughput |
| **Loki** | Centralized logs (all services push via Promtail or log driver) |
| **Jaeger** | Distributed traces (Spring Boot starter) |

**Per-Service Integration:**
- Micrometer + Prometheus actuator endpoint (`/actuator/prometheus`).
- Logback appender for Loki (structured JSON logs).
- OpenTelemetry for Jaeger tracing.

**Acceptance Criteria:**
- [ ] `make metrics` opens Grafana dashboard.
- [ ] Traces show full request flow (Gateway → Service → DB).
- [ ] Alerts configured for service down / high latency.

---

## Phase 10: Docker Compose & CI/CD
### Goal: One-command deploy.

**`docker-compose.yml` (Production):**
```yaml
services:
  nginx:
    image: nginx:alpine
    ports: ["80:80", "443:443"]
  api-gateway:
    build: ./api-gateway
    ports: ["8080:8080"]
  room-service:
    build: ./room-service
    ports: ["8081:8081"]
  # ... all services
  prometheus:
    image: prom/prometheus
  grafana:
    image: grafana/grafana
  loki:
    image: grafana/loki
  jaeger:
    image: jaegertracing/all-in-one
```

**GitHub Actions Pipeline:**
```yaml
on: push to main
jobs:
  test:
    - ./gradlew test
    - ./gradlew jacocoTestReport
  build:
    - ./gradlew bootJar
    - docker build -t airconsole/room-service:latest ./room-service
    - ... all services + games/*
  deploy:
    - ssh oracle-vm "docker-compose pull && docker-compose up -d"
```

**Game JAR Deployment:**
- Each game in `games/*` is built as a separate JAR via `./gradlew :games:snake:build`.
- `game-service` Dockerfile copies all game JARs into the classpath at build time.
- Adding a new game: `./gradlew :games:pong:build` → drop JAR into `game-service/build/libs/` → restart game-service. Zero downtime if using a rolling restart.

**Acceptance Criteria:**
- [ ] `docker-compose up -d` starts the entire platform.
- [ ] CI/CD pipeline builds, tests, and deploys on push.

---

## Phase 11: Polish & More Games
### Goal: Production edge cases + extensibility.

**Polish Features:**
| Feature | Description |
|---------|-------------|
| **Idempotency** | `X-Request-Id` header + Redis dedup (TTL 5m) on POST endpoints |
| **Invite Tokens** | Room creation generates `?invite=abc123` token. Prevents enumeration. |
| **Crockford Codes** | 5-char room codes (already in RoomCodeGenerator). |
| **Rate Limiting** | 10 req/s per IP on Gateway. |
| **Spectator Mode** | WebSocket join without controller permissions. |
| **Replay System** | Store every `GameInput` to Redis Stream → rebuild state on demand. |

**New Games (as independent Gradle modules in `games/`):**
1. **Pong** (2 players, paddles, ball physics, 16ms tick rate)
2. **Trivia** (questions, A/B/C/D answers, leaderboard per round, turn-based)

**How to Add a New Game (Zero changes to existing services):**
1. Create `games/your-game/build.gradle.kts`.
2. Implement `GameEngine` + `ControllerLayout`.
3. Add `SnakeRenderer.tsx` equivalent in `frontend/apps/screen/src/renderers/`.
4. Add `YourControllerLayout.tsx` in `frontend/apps/controller/src/layouts/`.
5. Register controller layout in `DynamicController.tsx` switch.
6. Build: `./gradlew :games:your-game:build`.
7. Deploy: game JAR to `game-service` classpath + frontend static files to Nginx.

**Acceptance Criteria:**
- [ ] Pong works with 2 players, 16ms tick loop.
- [ ] Trivia works with any number of players, turn-based.
- [ ] Replay system can reconstruct a game from stored inputs.
- [ ] **New game added without touching `common-lib`, `game-service` code, or other games' code.**

---

---

## Appendix A: Latency Budget & Performance

**End-to-End Target: < 50ms** from controller press to screen update.

| Stage | Budget | Technique |
|-------|--------|-----------|
| Controller press → SignalR | < 10ms | Azure SignalR WebSocket |
| SignalR → Notification Service | < 5ms | Same-AZ routing |
| Notification → Game Service | < 5ms | Redis Pub/Sub binary |
| Game Service input queue | < 2ms | Lock-free MPSC ring buffer (JCTools) |
| Game tick + snapshot | < 5ms | 0-allocation, single thread per room |
| Game Service → Redis broadcast | < 5ms | Binary protobuf Pub/Sub |
| Redis → Notification Service | < 5ms | Sub-microsecond local |
| Notification → SignalR | < 5ms | REST call (gzip, keep-alive) |
| SignalR → Screen | < 10ms | WebSocket |
| **Total** | **~ 47ms** | **All stages measured via Micrometer timers** |

**Additional Optimizations:**
1. **Binary Protocol**: `GameSnapshot` is protobuf, not JSON. ~90% size reduction.
2. **Single-Threaded Game Loop**: One `ScheduledExecutorService` per active room — no contention, no locks.
3. **No Redis Write Per Tick**: State lives in memory; flushed to Redis on checkpoint (every 10 ticks or player event).
4. **Connection Keep-Alive**: All HTTP clients (Notification → SignalR, Gateway → Services) use persistent connections.
5. **Pre-sized Buffers**: Input queues, snapshot buffers pre-allocated to avoid GC.
6. **Compression**: SignalR messages use MessagePack or gzip for WebSocket payloads.

---

## Appendix B: Controller Layout Contract (JSON Schema)

Backend sends this on `GET /api/games/{gameType}/layout`:
```json
{
  "gameType": "SNAKE",
  "screenSize": { "width": 800, "height": 600 },
  "buttons": [
    { "id": "up",    "label": "↑", "actionCode": 1,  "x": 50, "y": 0,  "w": 50, "h": 50, "style": "dpad" },
    { "id": "down",  "label": "↓", "actionCode": 2,  "x": 50, "y": 100,"w": 50, "h": 50, "style": "dpad" },
    { "id": "left",  "label": "←", "actionCode": 3,  "x": 0,  "y": 50, "w": 50, "h": 50, "style": "dpad" },
    { "id": "right", "label": "→", "actionCode": 4,  "x": 100,"y": 50, "w": 50, "h": 50, "style": "dpad" }
  ],
  "supportsTouch": true,
  "supportsKeyboard": true,
  "keyboardMap": {
    "ArrowUp": "up",
    "ArrowDown": "down",
    "ArrowLeft": "left",
    "ArrowRight": "right"
  }
}
```

Frontend `DynamicController.tsx` renders buttons dynamically from this JSON. **New games define their own layout with zero frontend code changes.**

---

## Appendix C: Gradle Module Structure (Groovy DSL)

**`settings.gradle`**
```groovy
rootProject.name = 'airconsole'

include 'common-lib'
include 'room-service'
include 'player-service'
include 'game-service'
include 'notification-service'
include 'score-service'
include 'api-gateway'
include 'games:snake'
include 'games:pong'
include 'games:trivia'
```

**`build.gradle`**
```groovy
plugins {
    id 'base'
}

allprojects {
    group = 'com.airconsole'
    version = '0.1.0-SNAPSHOT'
    repositories {
        mavenCentral()
    }
}
```

**`buildSrc/build.gradle`**
```groovy
plugins {
    id 'groovy-gradle-plugin'
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation 'io.spring.dependency-management:io.spring.dependency-management.gradle.plugin:1.1.6'
    implementation 'org.springframework.boot:org.springframework.boot.gradle.plugin:3.3.0'
}
```

**`buildSrc/src/main/groovy/java-conventions.gradle`**
```groovy
plugins {
    id 'java'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType(JavaCompile) {
    options.compilerArgs += ['-parameters']
}

tasks.withType(Test) {
    useJUnitPlatform()
    testLogging {
        events 'PASSED', 'FAILED', 'SKIPPED'
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly 'org.projectlombok:lombok:1.18.32'
    annotationProcessor 'org.projectlombok:lombok:1.18.32'
}
```

**`buildSrc/src/main/groovy/spring-boot-conventions.gradle`**
```groovy
plugins {
    id 'java-conventions'
    id 'org.springframework.boot'
    id 'io.spring.dependency-management'
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    
    runtimeOnly 'org.postgresql:postgresql'
    
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation 'org.testcontainers:redis'
}
```
  - room-service
  - player-service
  - game-service
  - notification-service
  - score-service
  - api-gateway
  - games:snake
  - games:pong
  - games:trivia
```

Each service applies:
```kotlin
plugins {
    id("java-conventions")
    id("spring-boot-conventions")
    id("testing-conventions")
}
```

Game modules apply only:
```kotlin
plugins {
    id("java-conventions")
    id("testing-conventions")
}
```
(Spring-free, pure Java — loaded by `game-service` at runtime.)

---

## Resume Hook (Copy-Paste)

> "AirConsole-inspired multiplayer gaming platform built with **Spring Boot microservices**, **event-driven architecture via Redis Streams**, **real-time communication via Azure SignalR** (< 50ms latency), **hexagonal domain layering**, **pluggable game engine system** (add games as JARs), and **full observability (Prometheus, Grafana, Loki, Jaeger)**. Frontend: React 19 + Vite + TypeScript monorepo with dynamic controller layouts. Build system: Gradle. Deployed via Docker Compose with CI/CD through GitHub Actions."
