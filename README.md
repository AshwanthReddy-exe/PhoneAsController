# 🎮 AirConsole Clone

> Phones become controllers. Browser tabs become the game screen.
>
> Multiplayer gaming platform built with Spring Boot microservices, event-driven architecture, and real-time WebSockets.

## Architecture Summary

| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.3 |
| Build | Gradle (Groovy DSL) |
| Frontend | React 19 + Vite + TypeScript |
| Messaging | Redis Streams + Pub/Sub |
| Real-Time | Azure SignalR |
| Database | PostgreSQL (persistent) |
| Cache/State | Redis (in-memory) |
| Observability | Prometheus + Grafana + Loki + Jaeger |
| Auth | JWT |
| Deployment | Docker Compose |

## Services

| Service | Port | Role |
|---------|------|------|
| API Gateway | 8080 | JWT validation, routing, rate limiting |
| Room Service | 8081 | Room lifecycle |
| Player Service | 8082 | Identity, sessions, reconnect |
| Game Service | 8083 | Game engine + Snake (pluggable) |
| Notification Service | 8084 | SignalR bridge |
| Score Service | 8085 | Leaderboards |

## Quick Start

```bash
# 1. Start the dev infrastructure
cd infra && docker-compose up -d

# 2. Build everything
./gradlew build

# 3. Run a service
./gradlew :room-service:bootRun
```

## One-Command Operations

```bash
make dev        # Start local dev stack (DB + Redis + Observability)
make test       # Run all tests
make build      # Build all modules
make clean      # Clean everything
make coverage   # Generate coverage reports
```

## Project Structure

```
airconsole/
├── common-lib/                 ← Shared DTOs, events, enums
├── api-gateway/                ← Single entry point
├── room-service/               ← Room lifecycle
├── player-service/             ← Identity + sessions
├── game-service/               ← Game engine + plugin system
├── notification-service/       ← SignalR bridge
├── score-service/              ← Leaderboards
├── games/
│   ├── snake/                  ← Snake game (independent JAR)
│   ├── pong/                   ← Pong game (future)
│   └── trivia/                 ← Trivia game (future)
├── frontend/
│   ├── apps/                   ← lobby, screen, controller
│   └── packages/               ← shared code
├── infra/                      ← Docker Compose stacks
└── buildSrc/                   ← Gradle convention plugins
```

## Games

Games are pluggable — each is an independent Gradle module implementing the `GameEngine` interface.

### Adding a New Game

1. Create `games/your-game/build.gradle`
2. Implement `GameEngine` + `ControllerLayout`
3. Add frontend renderer + controller layout
4. Build: `./gradlew :games:your-game:build`
5. Deploy JAR to game-service classpath

### Controller Layout

Games define their own controller layout dynamically:

```json
{
  "gameType": "SNAKE",
  "buttons": [
    { "id": "up", "label": "↑", "actionCode": 1, "x": 50, "y": 0, "w": 50, "h": 50 }
  ]
}
```

## License

MIT
