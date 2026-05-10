# AirConsole Clone — Hermes Session Context

> Auto-generated project context file.
> Captures the full discussion between the user and Hermes Agent regarding architecture decisions.
> Use this to resume work or hand off to another agent.

---

## 1. Original Request

The user provided two files in the working directory:
- `agent.md` — the original AirConsole clone architecture plan (793 lines)
- `changes.md` — 16 proposed architectural improvements over the initial design

User asked Hermes to **"read both files and wait for instructions"**.

---

## 2. Source Documents Summary

### `agent.md` — Base Architecture
- **Project**: AirConsole-inspired multi-device gaming platform
  - Phones = controllers, Browser tab = game screen
  - True microservices: each independently buildable, testable, deployable
- **Services**:
  - `common-lib` (shared DTOs, events, utils) — pure Java library
  - `api-gateway` (Spring Cloud Gateway, port 8080) — JWT validation, rate limiting, routing
  - `room-service` (8081, Neon PostgreSQL) — room lifecycle, 4-char codes, auto-expiry
  - `player-service` (8082, Neon PostgreSQL) — identity, sessions, reconnect with 60s window
  - `game-service` (8083, Upstash Redis) — game logic, tick loop, Snake/Pong/Trivia
  - `notification-service` (8084, stateless) — Azure SignalR bridge, broadcasts events
  - `score-service` (8085, Neon PostgreSQL) — leaderboards, history
- **Frontend**: lobby, screen, controller (Azure Static Web Apps)
- **Communication**:
  - REST via Gateway (inputs)
  - Redis Pub/Sub (events)
  - Azure SignalR (one-way broadcast)
- **Deployment**: Oracle Cloud 2x AMD VMs (free tier) + Azure services

### `changes.md` — Proposed Improvements
1. Replace Redis Pub/Sub with **Redis Streams** for critical events
2. Move game inputs from REST to **WebSockets** (SignalR → Notification → Game Service input queue)
3. Introduce **Game Engine Plugin System** (common `GameEngine` interface)
4. Separate Game Engine components internally (InputQueue, GameLoopScheduler, GameStateManager, EngineRegistry)
5. Add a **Game Catalog** (database table, not hardcoded enum)
6. Improve room code generation (Crockford Base32, 5 chars)
7. Add **Idempotency Protection** (request IDs in Redis)
8. Add **Invite Tokens** (prevents room enumeration)
9. Add **Spectator Mode**
10. Add **Replay System** (store every input event)
11. Add **Analytics & Metrics** (Prometheus metrics)
12. Improve Frontend monorepo structure (apps/ + shared/)
13. **Simplify initial deployment** (single Oracle VM via Docker Compose)
14. Explicit **Game Plugin Contract**
15. Future stretch goal: **Custom Game SDK**
16. Keep / Remove decisions matrix

---

## 3. User's Core Requirement

> "I don't want to overcomplicate it... but it should be one of the best projects in my resume with very good amount of skills and tools included!! ... what do you suggest so this becomes very good in a not-so-overly-complicated way."

**User Preference**: minimal, practical, deterministic designs that avoid overengineering.
**User Style**: likes step-by-step collaboration with confirmation between stages.

---

## 4. Hermes Recommendations (The Sweet Spot)

### What ALREADY impresses on a resume (keep from agent.md)
- Hexagonal / layered architecture (`api/`, `domain/`, `infrastructure/`)
- Event-driven microservices
- TestContainers for integration testing
- JWT gateway auth + rate limiting
- Real-time WebSockets via Azure SignalR
- GitHub Actions + Docker Compose CI/CD
- Clean separation of concerns per service

### What to ADD for "production-level" impact
1. **Redis Streams** (changes.md #1) instead of Kafka
   - Kafka is a resume buzzword but operational nightmare for a student project.
   - Redis Streams gives: durability, consumer groups, replay.
   - Humble-brag version: *"chose Redis Streams over Kafka for operational simplicity with a documented migration path."*

2. **WebSocket inputs** (changes.md #2)
   - Drop the REST-only input path for controllers.
   - SignalR → Notification Service → Game Service input queue.
   - Symmetric comms, lower latency, impressive architecture.

3. **Game Engine Plugin System** (changes.md #3)
   - `GameEngine` interface + registry mapping `GameType → GameEngine`.
   - Pure resume gold. Shows OCP, strategy pattern, clean extensibility.

4. **Observability Stack** (building on agent.md)
   - Already planned: Prometheus + Grafana
   - **Add Loki** for centralized logs (one Docker Compose line, looks like ELK without JVM hell)
   - **Add Jaeger** for distributed tracing (lightweight across microservices)
   - Impressive to say: *"full observability — metrics, logs, and traces."*

5. **Idempotency + Invite Tokens** (changes.md #7, #8)
   - Small features that signal edge-case thinking.
   - Request-ID header + Redis dedup / room invite token prevents enumeration.

6. **Single VM deployment** (changes.md #13)
   - Start with Docker Compose on one Oracle VM.
   - Mention horizontal scaling / K8s as future work in ADRs.

7. **Crockford Base32 room codes** (changes.md #6)
   - 5-char codes, 33M combinations, no visual confusion.
   - Tiny change, shows attention to detail.

### What to SKIP (overcomplication with low ROI)
- **Kafka** — operational cost >> resume value. Mention as "future migration path" in ADR.
- **ELK stack** — Loki replaces it with 10% the pain.
- **Kubernetes** — Docker Compose is fine. Mention K8s as future work.
- **Multi-VM from day one** — start single VM, split only if needed.
- **Spectator mode** — stretch goal after core.
- **Replay system** — stretch goal after core.
- **Custom Game SDK** — future vision, not MVP.
- **Game Catalog DB table** — fine to hardcode 3 games for MVP; add catalog later.

---

## 5. Resume Impact Summary

**What this project demonstrates:**
| Skill | Where it shows |
|-------|----------------|
| Microservices architecture | 6 services + gateway |
| Event-driven design | Redis Streams pub/sub |
| Real-time systems | SignalR WebSockets |
| Clean / Hexagonal architecture | Domain layers, ports-adapters |
| Design patterns | Plugin engine (Strategy + Registry) |
| Authentication & security | JWT gateway validation, invite tokens |
| Caching & performance | Redis for game state + Streams |
| Observability | Prometheus + Grafana + Loki + Jaeger |
| CI/CD | GitHub Actions + Docker Compose |
| Testing strategy | Unit + Integration with TestContainers |
| Cloud deployment | Oracle Cloud + Azure services |
| API design | RESTful + event contracts |

**One-line resume hook:**
> "AirConsole-inspired multiplayer gaming platform built with Spring Boot microservices, event-driven architecture via Redis Streams, real-time communication via Azure SignalR, hexagonal domain layering, and full observability (Prometheus, Grafana, Loki, Jaeger). Deployed via Docker Compose with CI/CD through GitHub Actions."

---

## 6. Suggested Execution Order

Follow this order to avoid getting stuck:

1. **common-lib** — DTOs, events, enums, utils
2. **room-service** — simplest, no service dependencies
3. **player-service** — depends on room events
4. **game-service** — with plugin engine + one game (Snake)
5. **notification-service** + **Azure SignalR** integration
6. **score-service** — consume game.finished events
7. **api-gateway** — JWT, rate limiting, routing table
8. **Frontend** — lobby, screen, controller (vanilla JS first)
9. **Observability** — Prometheus, Grafana, Loki, Jaeger
10. **Docker Compose** + **GitHub Actions** CI/CD
11. **Polish** — idempotency, invite tokens, Crockford codes, rate limiting
12. **Stretch** — Pong, Trivia, game catalog, spectator, replay

**Per-service dev flow:**
```
domain logic → unit tests → infrastructure → integration tests → API layer
```

---

## 7. Tools & Technologies Matrix

| Layer | Tech |
|-------|------|
| Language | Java 21+ |
| Framework | Spring Boot 3.x |
| Gateway | Spring Cloud Gateway |
| Database (persistent) | Neon PostgreSQL |
| Cache / State / Events | Upstash Redis (Streams + Pub/Sub) |
| WebSockets | Azure SignalR |
| Frontend | Vanilla JS / lightweight framework |
| Messaging | Redis Streams (critical), Redis Pub/Sub (real-time) |
| Auth | JWT (Player Service issues, Gateway validates) |
| Metrics | Prometheus |
| Dashboards | Grafana |
| Logs | Loki |
| Traces | Jaeger |
| Testing | JUnit 5 + Mockito + TestContainers |
| CI/CD | GitHub Actions |
| Deployment | Docker Compose on Oracle Cloud VM |
| DNS / SSL | Cloudflare |

---

## 8. Key Files in Working Directory

- `agent.md` — original full architecture specification
- `changes.md` — proposed 16 enhancements
- `HERMES_CONTEXT.md` — this file (session context + decisions)

---

## 9. Open Decisions / Next Steps

(Awaiting user instructions. Pick one:)
- Start generating the project structure / boilerplate?
- Draft refined architecture doc incorporating these decisions?
- Start implementing a specific service?
- Set up Docker Compose / infra first?
- Something else?

---

*Generated by Hermes Agent. If resuming this project, read this file first, then `agent.md`, then `changes.md`.*
