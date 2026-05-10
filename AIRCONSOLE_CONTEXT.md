# AirConsole Clone — Compressed Context

## What This Is
AirConsole-inspired multiplayer platform. Phones = controllers. Browser = game screen.
6 Spring Boot microservices + gateway. Event-driven via Redis Streams. Real-time via Azure SignalR.

## Decisions Already Made
- **Messaging**: Redis Streams (critical events), Redis Pub/Sub (real-time game.state.updated)
- **Skip**: Kafka, ELK, K8s, multi-VM day-1, spectator, replay, custom SDK
- **Add**: Loki (logs), Jaeger (traces), WebSocket inputs, GameEngine plugin system, idempotency, invite tokens, Crockford Base32 codes
- **Deploy**: Single Oracle VM via Docker Compose

## Services & Ports
| Service | Port | DB | Role |
|---------|------|-----|------|
| api-gateway | 8080 | none | JWT, routing, rate limit |
| room-service | 8081 | Neon PG | room lifecycle |
| player-service | 8082 | Neon PG | identity, 60s reconnect window |
| game-service | 8083 | Upstash Redis | game loop, plugin engine |
| notification-service | 8084 | none | SignalR bridge |
| score-service | 8085 | Neon PG | leaderboards |

## Build Order (phase-wise)
Phase 1: common-lib + room-service
Phase 2: player-service
Phase 3: game-service (Snake only first)
Phase 4: notification-service + SignalR
Phase 5: score-service
Phase 6: api-gateway
Phase 7: frontend (lobby/screen/controller)
Phase 8: observability (Prometheus/Grafana/Loki/Jaeger)
Phase 9: Docker Compose + GitHub Actions CI/CD
Phase 10: Polish (idempotency, invite tokens, Crockford codes, rate limiting)
Phase 11: More games (Pong, Trivia), game catalog

## Per-Service Dev Flow
domain logic -> unit tests -> infrastructure -> integration tests -> API layer

## Resume Hook
"Spring Boot microservices, Redis Streams event bus, Azure SignalR real-time, hexagonal architecture, full observability (Prometheus/Grafana/Loki/Jaeger), Docker Compose deployment."

## Next Step
Awaiting user instruction on which phase to start.
