# AirConsole — Agent Architecture Plan

## Project Overview
AirConsole-inspired multi-device gaming platform.
Phones become game controllers. Browser tab becomes the game screen.
Built as true microservices — each service independently buildable, testable, deployable.

---

## Monorepo Structure

```
airconsole/
├── common-lib/
├── api-gateway/
├── room-service/
├── player-service/
├── game-service/
├── notification-service/
├── score-service/
├── frontend/
│   ├── lobby/
│   ├── screen/
│   └── controller/
├── infra/
│   ├── docker-compose.yml
│   ├── docker-compose.prod.yml
│   └── nginx/
├── docs/
│   ├── adr/
│   ├── api-contracts.md
│   └── architecture.md
├── pom.xml  ← parent pom
└── agent.md ← this file
```

---

## Service Port Map

| Service              | Port |
|----------------------|------|
| API Gateway          | 8080 |
| Room Service         | 8081 |
| Player Service       | 8082 |
| Game Service         | 8083 |
| Notification Service | 8084 |
| Score Service        | 8085 |
| Prometheus           | 9090 |
| Grafana              | 3000 |

---

## Build Order

```
① common-lib           → shared DTOs, events, utils (no Spring Boot)
② room-service         → simplest, no service dependencies
③ player-service       → depends on room events
④ game-service         → depends on player events
⑤ notification-service → depends on all events
⑥ score-service        → depends on game.finished event
⑦ api-gateway          → routes to all services
⑧ frontend             → lobby + screen + controller
```

---

## 1. Common Library (`common-lib`)

Not a Spring Boot app. Pure Java library.
Imported by all services via Maven.

```
common-lib/src/main/java/com/airconsole/common/
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
│   ├── GameStartedEvent.java
│   ├── GameStateUpdatedEvent.java
│   └── GameFinishedEvent.java
├── enums/
│   ├── RoomStatus.java       → WAITING, PLAYING, FINISHED, EXPIRED
│   ├── PlayerRole.java       → HOST, PLAYER
│   ├── PlayerStatus.java     → CONNECTED, DISCONNECTED
│   ├── GameType.java         → SNAKE, PONG, TRIVIA
│   └── GameStatus.java       → WAITING, RUNNING, PAUSED, FINISHED
└── utils/
    ├── RoomCodeUtil.java     → generates 4-char codes e.g. "XKQP"
    └── JwtUtil.java
```

---

## 2. Room Service (`room-service`)

**Port:** 8081
**Database:** Neon PostgreSQL — schema: `rooms`
**Responsibility:** Room lifecycle only. Knows nothing about players or game state.

### Domain Model
```
Room {
  roomId      : UUID
  roomCode    : String (4 chars, unique e.g. "XKQP")
  status      : RoomStatus (WAITING / PLAYING / FINISHED / EXPIRED)
  hostId      : UUID (playerId of creator)
  gameType    : GameType (SNAKE / PONG / TRIVIA)
  maxPlayers  : int
  playerCount : int (denormalized — updated via events)
  createdAt   : timestamp
  expiresAt   : timestamp (createdAt + 30 mins)
}
```

### REST API
```
POST /api/rooms/create
  Body: { gameType, maxPlayers }
  Response: { roomId, roomCode, status, expiresAt }

POST /api/rooms/join
  Body: { roomCode }
  Response: { roomId, roomCode, status, playerCount }

GET  /api/rooms/{roomCode}/status
  Response: { roomId, status, playerCount, maxPlayers, gameType }

DELETE /api/rooms/{roomId}
  → host only, validated at gateway
```

### Events Published (Redis Pub/Sub)
```
room.created   → { roomId, roomCode, gameType, hostId }
room.expired   → { roomId, roomCode }
```

### Events Consumed
```
player.joined        → increment playerCount
player.left          → decrement playerCount
```

### Business Rules
- Room code is unique and 4 chars (A-Z only)
- Room auto-expires after 30 mins of inactivity
- Room Service publishes `room.expired` → all other services clean up their own data
- playerCount is maintained via events, not cross-service calls

### Internal Structure
```
room-service/src/main/java/com/airconsole/room/
├── RoomServiceApplication.java
├── api/
│   ├── RoomController.java
│   ├── request/
│   │   ├── CreateRoomRequest.java
│   │   └── JoinRoomRequest.java
│   └── response/
│       └── RoomResponse.java
├── domain/
│   ├── Room.java
│   └── RoomService.java
├── infrastructure/
│   ├── persistence/
│   │   ├── RoomEntity.java
│   │   ├── RoomJpaRepository.java
│   │   └── RoomPersistenceAdapter.java
│   ├── messaging/
│   │   ├── RoomEventPublisher.java
│   │   └── RoomEventConsumer.java
│   └── config/
│       └── RedisConfig.java
└── exception/
    ├── RoomNotFoundException.java
    ├── RoomFullException.java
    └── GlobalExceptionHandler.java
```

### Testing
```
Unit:        RoomServiceTest.java        → mock all dependencies
Integration: RoomControllerIT.java       → TestContainers (real PostgreSQL + Redis)
```

---

## 3. Player Service (`player-service`)

**Port:** 8082
**Database:** Neon PostgreSQL — schema: `players`
**Responsibility:** Player identity, session, connection state, rejoin logic.

### Domain Model
```
Player {
  playerId     : UUID
  roomId       : UUID
  playerName   : String
  role         : PlayerRole (HOST / PLAYER)
  status       : PlayerStatus (CONNECTED / DISCONNECTED)
  connectionId : String (Azure SignalR connection ID)
  joinedAt     : timestamp
  lastSeenAt   : timestamp (updated on disconnect, used for rejoin window)
}
```

### REST API
```
POST /api/players/register
  Body: { roomId, playerName }
  Response: { playerId, token (JWT), role }

POST /api/players/reconnect
  Body: { playerId, roomId }
  Response: { playerId, token (JWT), connectionId }

GET  /api/players/{roomId}/all
  Response: [{ playerId, playerName, role, status }]

PATCH /api/players/{playerId}/connection
  Body: { connectionId }   ← called by Notification Service on WS connect
  Response: 200 OK
```

### Events Published
```
player.joined         → { playerId, roomId, playerName, role }
player.disconnected   → { playerId, roomId, lastSeenAt }
player.rejoined       → { playerId, roomId }
player.left           → { playerId, roomId }  (after rejoin window expires)
```

### Events Consumed
```
room.expired → remove all players in that room
```

### Rejoin Logic
```
1. Phone disconnects (SignalR disconnect detected)
2. Notification Service calls PATCH /api/players/{playerId}/connection with null connectionId
3. Player Service sets status = DISCONNECTED, lastSeenAt = now
4. Player Service publishes player.disconnected event
5. Game Service pauses game on receiving this event
6. Within 60 seconds: phone reconnects → POST /api/players/reconnect
7. Player Service sets status = CONNECTED, new connectionId
8. Player Service publishes player.rejoined event
9. Game Service resumes
10. After 60 seconds with no rejoin: scheduled job publishes player.left
```

### Internal Structure
```
player-service/src/main/java/com/airconsole/player/
├── PlayerServiceApplication.java
├── api/
│   ├── PlayerController.java
│   └── request/ + response/
├── domain/
│   ├── Player.java
│   ├── PlayerService.java
│   └── RejoinWindowScheduler.java   ← checks lastSeenAt every 10s
├── infrastructure/
│   ├── persistence/
│   ├── messaging/
│   │   ├── PlayerEventPublisher.java
│   │   └── PlayerEventConsumer.java
│   └── config/
└── exception/
```

### Testing
```
Unit:        RejoinWindowSchedulerTest.java  → test 60s logic
Integration: PlayerControllerIT.java         → TestContainers
```

---

## 4. Game Service (`game-service`)

**Port:** 8083
**Storage:** Upstash Redis (game state — needs speed, not persistence)
**Responsibility:** Game logic, input processing, game loop, scoring.

### Domain Model (stored in Redis as JSON)
```
GameState {
  gameId     : UUID
  roomId     : UUID
  gameType   : GameType
  gameStatus : GameStatus (WAITING / RUNNING / PAUSED / FINISHED)
  players    : Map<playerId, PlayerGameState>
  scores     : Map<playerId, Integer>
  tickNumber : long
  startedAt  : timestamp
  finishedAt : timestamp
}

PlayerGameState (Snake example) {
  playerId  : UUID
  positions : List<{x, y}>   ← snake body
  direction : String          ← UP/DOWN/LEFT/RIGHT
  alive     : boolean
}
```

### REST API
```
POST /api/games/start
  Body: { roomId, gameType }
  Response: { gameId, status }

POST /api/games/{roomId}/input
  Body: { playerId, input }   ← LEFT / RIGHT / UP / DOWN / A / B / C / D
  Response: 200 OK

GET  /api/games/{roomId}/state
  Response: { gameState, scores, status }
```

### Game Loop
```
- Server-side tick every 100ms (Snake), 16ms (Pong)
- Game loop runs as scheduled thread in Game Service
- Each tick: process queued inputs → update state → publish game.state.updated
- Game Service does NOT push directly to clients → only publishes events
```

### Games
```
Snake:
  → Each player controls one snake
  → Last alive wins
  → Input: UP / DOWN / LEFT / RIGHT

Pong:
  → 2 players, one paddle each
  → First to 5 points wins
  → Input: LEFT / RIGHT

Trivia:
  → Screen shows question
  → Phones show A / B / C / D buttons
  → Fastest correct answer = most points
  → Input: A / B / C / D
```

### Events Published
```
game.started          → { gameId, roomId, gameType }
game.state.updated    → { roomId, gameState, scores, tickNumber }
game.paused           → { roomId, reason: "player.disconnected" }
game.resumed          → { roomId }
game.finished         → { roomId, gameId, winnerId, finalScores }
```

### Events Consumed
```
player.disconnected   → pause game
player.rejoined       → resume game
player.left           → remove from game, continue or end
room.expired          → terminate game
```

### Internal Structure
```
game-service/src/main/java/com/airconsole/game/
├── GameServiceApplication.java
├── api/
│   ├── GameController.java
│   └── request/ + response/
├── domain/
│   ├── GameState.java
│   ├── GameService.java
│   ├── GameLoopScheduler.java       ← ticks every N ms
│   └── games/
│       ├── Game.java                ← interface
│       ├── SnakeGame.java
│       ├── PongGame.java
│       └── TriviaGame.java
├── infrastructure/
│   ├── cache/
│   │   └── GameStateRedisAdapter.java
│   ├── messaging/
│   │   ├── GameEventPublisher.java
│   │   └── GameEventConsumer.java
│   └── config/
└── exception/
```

### Testing
```
Unit:        SnakeGameTest.java    → test collision, movement, scoring
Integration: GameControllerIT.java → TestContainers Redis
```

---

## 5. Notification Service (`notification-service`)

**Port:** 8084
**Storage:** None (fully stateless)
**Responsibility:** Bridge between internal events and external WebSocket clients.

### What it does
```
1. Subscribes to ALL Redis events from all services
2. Maps roomId → Azure SignalR group
3. Broadcasts to correct group
4. Detects WebSocket connect/disconnect
5. Notifies Player Service of connection changes
```

### WebSocket Flow
```
Phone/Screen connects to Azure SignalR
  → SignalR triggers connect webhook → Notification Service
  → Notification Service calls Player Service:
    PATCH /api/players/{playerId}/connection { connectionId }
  → Player Service updates connectionId

Phone/Screen disconnects
  → SignalR triggers disconnect webhook → Notification Service
  → Notification Service calls Player Service:
    PATCH /api/players/{playerId}/connection { connectionId: null }
  → Player Service starts rejoin window
```

### Events Consumed → SignalR Broadcast
```
room.created          → notify host: room ready
player.joined         → broadcast to room: player list updated
player.disconnected   → broadcast to room: player disconnected
player.rejoined       → broadcast to room: player back
game.started          → broadcast to room: game starting
game.state.updated    → broadcast to room: { gameState, scores }
game.paused           → broadcast to room: game paused
game.resumed          → broadcast to room: game resumed
game.finished         → broadcast to room: { winnerId, finalScores }
room.expired          → broadcast to room: room closed
```

### SignalR Groups
```
Each room = one SignalR group named by roomCode e.g. "XKQP"
Screen joins group "XKQP"
All phones in room join group "XKQP"
Notification Service broadcasts to group "XKQP"
```

### Internal Structure
```
notification-service/src/main/java/com/airconsole/notification/
├── NotificationServiceApplication.java
├── signalr/
│   ├── SignalRPublisher.java        ← sends to Azure SignalR REST API
│   └── SignalRWebhookController.java ← receives connect/disconnect
├── consumer/
│   └── EventConsumer.java           ← subscribes to all Redis topics
├── mapper/
│   └── EventToMessageMapper.java    ← maps events to WS messages
└── config/
    └── RedisConfig.java
```

### Testing
```
Unit: EventConsumerTest.java → mock SignalR, verify correct group targeting
```

---

## 6. Score Service (`score-service`)

**Port:** 8085
**Database:** Neon PostgreSQL — schema: `scores`
**Responsibility:** Persist final scores, serve leaderboard and match history.

### Domain Model
```
Score {
  scoreId    : UUID
  roomId     : UUID
  gameId     : UUID
  playerId   : UUID
  playerName : String
  score      : int
  rank       : int
  gameType   : GameType
  playedAt   : timestamp
}
```

### REST API
```
GET /api/scores/leaderboard/{gameType}
  Response: [{ playerName, score, rank, playedAt }]

GET /api/scores/history/{roomId}
  Response: [{ gameId, gameType, scores[], playedAt }]
```

### Events Consumed
```
game.finished → persist finalScores for all players
```

### Internal Structure
```
score-service/src/main/java/com/airconsole/score/
├── ScoreServiceApplication.java
├── api/
│   └── ScoreController.java
├── domain/
│   ├── Score.java
│   └── ScoreService.java
├── infrastructure/
│   ├── persistence/
│   └── messaging/
│       └── ScoreEventConsumer.java
└── exception/
```

---

## 7. API Gateway (`api-gateway`)

**Port:** 8080
**Storage:** None (stateless)
**Tech:** Spring Cloud Gateway

### Responsibilities
```
→ Single entry point for ALL HTTP clients
→ Validate JWT on every request (except /create and /join)
→ Route to correct service
→ Rate limiting (per IP)
→ CORS handling
```

### Route Table
```
POST /api/rooms/create          → room-service:8081   (no auth)
POST /api/rooms/join            → room-service:8081   (no auth)
GET  /api/rooms/**              → room-service:8081   (JWT required)
POST /api/players/register      → player-service:8082 (no auth)
POST /api/players/reconnect     → player-service:8082 (JWT required)
GET  /api/players/**            → player-service:8082 (JWT required)
POST /api/games/start           → game-service:8083   (JWT required, HOST only)
POST /api/games/**/input        → game-service:8083   (JWT required)
GET  /api/games/**              → game-service:8083   (JWT required)
GET  /api/scores/**             → score-service:8085  (no auth)
```

### JWT Flow
```
1. Player registers → Player Service issues JWT
   JWT payload: { playerId, roomId, role, exp }

2. Every request → Gateway validates JWT
3. Gateway injects playerId into request header
4. Downstream services trust header (no re-validation)
```

---

## 8. Frontend

**Hosting:** Azure Static Web Apps (free, always on, global CDN)

### lobby/index.html
```
→ Create Room button → POST /api/rooms/create
→ Enter room code input → POST /api/rooms/join
→ On success → redirect to screen.html or controller.html
→ Show QR code for easy phone joining
```

### screen/screen.html
```
→ Connects to Azure SignalR group (roomCode)
→ Shows room code + QR code while waiting
→ Shows connected players list
→ On game.started → loads correct game canvas
→ Receives game.state.updated → renders frame
→ Shows scores on game.finished
```

### controller/controller.html
```
→ Connects to Azure SignalR group (roomCode)
→ POST /api/players/register on load
→ Shows game-specific buttons:
   Snake:  D-PAD (UP / DOWN / LEFT / RIGHT)
   Pong:   LEFT / RIGHT only
   Trivia: A / B / C / D buttons
→ On button press → POST /api/games/{roomId}/input
→ Shows own score + rank
```

---

## 9. Communication Architecture

### Synchronous (REST via Gateway)
```
Client → Gateway → Room Service     (create room, join room)
Client → Gateway → Player Service   (register, reconnect)
Client → Gateway → Game Service     (start game, send input)
Client → Gateway → Score Service    (leaderboard, history)
```

### Asynchronous (Redis Pub/Sub Topics)
```
Topic                  Producer           Consumers
─────────────────────────────────────────────────────────────
room.created           Room Svc           Notification
room.expired           Room Svc           Game, Player, Notification
player.joined          Player Svc         Room, Game, Notification
player.disconnected    Player Svc         Game, Notification
player.rejoined        Player Svc         Game, Notification
player.left            Player Svc         Room, Game, Notification
game.started           Game Svc           Notification
game.state.updated     Game Svc           Notification
game.paused            Game Svc           Notification
game.resumed           Game Svc           Notification
game.finished          Game Svc           Score, Notification
```

### WebSocket (Azure SignalR)
```
Direction:  Notification Service → Azure SignalR → Phone + Screen
Protocol:   SignalR (WebSocket under the hood)
Groups:     One group per room, named by roomCode
Direction:  ONE WAY — server pushes to clients only!

Phone sends inputs → REST (not WebSocket!)
Server pushes state → WebSocket (SignalR only!)
```

---

## 10. Data Storage

| Service              | Storage             | Why                          |
|----------------------|---------------------|------------------------------|
| Room Service         | Neon PostgreSQL     | Persistent room records      |
| Player Service       | Neon PostgreSQL     | Persistent player sessions   |
| Game Service         | Upstash Redis       | Fast in-memory game state    |
| Score Service        | Neon PostgreSQL     | Persistent score history     |
| Notification Service | None                | Stateless                    |
| API Gateway          | None                | Stateless                    |
| Shared               | Upstash Redis       | JWT blacklist, rate limiting |

---

## 11. Deployment Architecture

### Oracle AMD VM1 (free forever)
```
→ Nginx (reverse proxy, port 80/443)
→ API Gateway (:8080)
→ Room Service (:8081)
→ Player Service (:8082)
```

### Oracle AMD VM2 (free forever)
```
→ Game Service (:8083)
→ Notification Service (:8084)
→ Score Service (:8085)
→ Prometheus (:9090)
→ Grafana (:3000)
```

### Cloud Services (free tiers)
```
→ Neon PostgreSQL      (database)
→ Upstash Redis        (cache + pub/sub)
→ Azure SignalR        (WebSockets, 20 concurrent free)
→ Azure Static Web Apps (frontend, always on)
→ Cloudflare           (DNS + SSL)
→ GitHub Actions       (CI/CD)
```

### JVM Tuning (fits in 1GB RAM per VM)
```
Each service: -Xmx128m -Xms64m -XX:+UseSerialGC
Estimated RAM per VM: ~500MB used / 1GB available
```

---

## 12. Each Service Internal Layer Pattern

Every service follows identical layering:

```
api/           ← HTTP controllers, request/response DTOs
domain/        ← pure business logic, no Spring annotations
infrastructure/
  persistence/ ← JPA entities, repositories
  messaging/   ← Redis publishers and consumers
  config/      ← Spring config classes
exception/     ← custom exceptions, global handler
```

---

## 13. Testing Strategy

```
Unit Tests (no DB, no Redis):
→ Test domain logic only
→ Mock all infrastructure
→ Fast, run on every commit

Integration Tests (real DB + Redis):
→ TestContainers spins up real PostgreSQL + Redis
→ Test full HTTP request → DB flow
→ Each service tested in complete isolation

Run single service tests:
cd room-service && mvn test
cd game-service && mvn verify
```

---

## 14. Architecture Decision Records (ADRs)

Write one ADR per major decision in `/docs/adr/`:

```
ADR-001: Redis Pub/Sub over Kafka
ADR-002: REST for inputs, SignalR only for broadcasts
ADR-003: Server-side game loop over client-side
ADR-004: Neon PostgreSQL over self-hosted
ADR-005: Upstash Redis over self-hosted
ADR-006: Soft disconnect with 60s rejoin window
ADR-007: playerCount denormalization in Room Service
ADR-008: JWT issued by Player Service, validated at Gateway
```

---

## 15. CI/CD (GitHub Actions)

```yaml
On push to main:
  → mvn package (all modules)
  → docker build each service
  → ssh into Oracle VM
  → docker-compose up -d --build
```

---

## 16. Observability

```
Prometheus → scrapes metrics from all services
Grafana    → dashboards showing:
  → Active rooms count
  → Connected players count
  → Game inputs per second
  → Average input-to-broadcast latency
  → Service health / uptime
  → Redis pub/sub message rate
```

---

## Summary

```
Services:       6 Spring Boot microservices + 1 gateway
Games:          Snake, Pong, Trivia
Communication:  REST (inputs) + Redis Pub/Sub (events) + SignalR (broadcasts)
Database:       Neon PostgreSQL + Upstash Redis
Deployment:     Oracle Cloud (2 AMD VMs) + Azure + Cloudflare
Cost:           ₹0/month forever
```
