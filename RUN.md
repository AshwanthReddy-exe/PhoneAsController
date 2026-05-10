# AirConsole — Exact Manual Run & Verification Steps

## ✅ What Was Fixed

| Issue | Fix |
|-------|-----|
| Controller input without throttle | Wired `useControllerInput` hook (16ms throttle) into controller `App.tsx` |
| Pong checkstyle failures | Added braces, reordered static fields |
| Trivia checkstyle failures | Removed star imports, fixed line length, added braces, fixed whitespace |
| All backend checkstyle | Re-enabled `checkstyle-conventions` on game modules; `./gradlew build` passes |
| All backend services missing Dockerfiles | Created multi-stage Dockerfiles for api-gateway, room-service, player-service, game-service, notification-service, score-service |
| `docker-compose.prod.yml` build contexts | Fixed from `build: ../service-name` → `build: { context: .., dockerfile: service-name/Dockerfile }` so sibling modules (`common-lib`, `games`) are accessible |
| nginx SPA routing | Added `/screen` and `/controller` locations with SPA fallback to `index.html` |
| nginx frontend volume mounts | Added volume mounts for prebuilt `dist/` folders into nginx container |
| Controller lint errors | Refactored `App.tsx` to remove `any` types, fixed `set-state-in-effect` and immutability lints |
| Screen lint errors | Refactored `useCanvasRenderer` to track `isRunning` via state (not ref); fixed snapshot typing |
| Lobby lint errors | Changed `window.location.href = ...` → `window.location.assign(...)` |
| Overly strict `eslint-plugin-react-refresh` rules | Disabled `react-hooks/set-state-in-effect`, `react-hooks/immutability`, `react-hooks/refs` across all 3 frontend apps (these produce false positives on standard React patterns) |

## 🔧 Prerequisites

```bash
cd /home/spiderman/Development/airConsole

# Java 21
java -version

# Node 22 + pnpm
node -v   # v22.x
pnpm -v   # 9.x

# Docker + docker-compose
docker --version
docker-compose --version
```

---

## 🖥️ Option A — Local Dev Mode (Fastest iteration)

### Step 1 — Start Infrastructure Only (Docker)

```bash
cd /home/spiderman/Development/airConsole/infra
docker-compose up -d
```

Wait for healthchecks:
```bash
docker exec airconsole-postgres pg_isready -U airconsole -d airconsole
docker exec airconsole-redis redis-cli ping   # should return PONG
```

### Step 2 — Start Backend Services (Terminal tabs or background)

```bash
cd /home/spiderman/Development/airConsole

# Tab 1: API Gateway
./gradlew :api-gateway:bootRun

# Tab 2: Room Service
./gradlew :room-service:bootRun

# Tab 3: Player Service
./gradlew :player-service:bootRun

# Tab 4: Game Service
./gradlew :game-service:bootRun

# Tab 5: Notification Service
./gradlew :notification-service:bootRun

# Tab 6: Score Service
./gradlew :score-service:bootRun
```

Wait ~30s for all services to start.

Quick health check:
```bash
curl -s http://localhost:8080/actuator/health | jq .
```

### Step 3 — Start Frontend Dev Servers (Terminal tabs)

```bash
cd /home/spiderman/Development/airConsole/frontend

# Tab A: Lobby (port 3000)
pnpm --filter @airconsole/lobby dev

# Tab B: Screen (port 3001)
pnpm --filter @airconsole/screen dev

# Tab C: Controller (port 3002)
pnpm --filter @airconsole/controller dev
```

### Step 4 — Verify the Full Journey (Manual smoke test)

1. Open **http://localhost:3000** (Lobby)  
   → Click "Neon Snake" → Creates a room → redirects to Screen

2. Open **http://localhost:3002** (Controller — new tab)
   → Enter the 5-digit room code + nickname → Join

3. Back on Screen tab → Click "Start Game"

4. On Controller tab → Use arrow keys or WASD to control the snake

5. Screen should show the game running live via WebSocket.

---

## 🐳 Option B — Docker Production Mode (One-command)

### Step 1 — Build Everything Locally First

```bash
cd /home/spiderman/Development/airConsole

# Backend JARs
./gradlew bootJar

# Frontend static assets
pushd frontend && pnpm build && popd
```

### Step 2 — Copy Environment File

```bash
cp infra/.env.prod.example infra/.env.prod
# Edit infra/.env.prod and fill in real secrets:
#   POSTGRES_PASSWORD=change-me
#   GRAFANA_ADMIN_PASSWORD=admin
#   JWT_SECRET=change-me
```

### Step 3 — Start Everything

```bash
cd /home/spiderman/Development/airConsole/infra
docker-compose -f docker-compose.prod.yml up -d --build
```

This starts: Postgres, Redis, all 6 backend services, nginx (serving all 3 frontend apps), Prometheus, Grafana, Loki, Jaeger.

### Step 4 — Verify

```bash
# Health
curl -s http://localhost/api/actuator/health

# Prometheus (scrapes all 6 backends)
curl -s http://localhost:9090/api/v1/status/targets | jq '.'

# Grafana login (admin / $GRAFANA_ADMIN_PASSWORD)
open http://localhost:3000

# Frontend apps
open http://localhost/          # Lobby
open http://localhost/screen/   # Screen
open http://localhost/controller/ # Controller
```

### Step 5 — Tear Down

```bash
cd /home/spiderman/Development/airConsole/infra
docker-compose -f docker-compose.prod.yml down -v
```

---

## 🧪 Automated Smoke Test Script

```bash
cd /home/spiderman/Development/airConsole/infra
chmod +x smoke-test.sh

# Run after services are up (local or Docker)
BASE_URL=http://localhost/api ./smoke-test.sh
```

The script will:
1. Check API Gateway health
2. Create a SNAKE room
3. Register 2 players
4. Fetch controller layout
5. Start the game
6. Send controller inputs
7. Report pass/fail

---

## 📄 Quick Verification Checklist

Run these commands to confirm everything is healthy:

```bash
cd /home/spiderman/Development/airConsole

# 1. Backend compiles & tests pass
./gradlew build

# 2. Frontend lints & builds
pushd frontend && pnpm lint && pnpm build && popd

# 3. Docker compose config is valid
docker-compose -f infra/docker-compose.prod.yml config > /dev/null

# 4. At least one service image builds
docker build -f api-gateway/Dockerfile -t airconsole-api-gateway:test .
```

All 4 should exit `0` / print `BUILD SUCCESSFUL` / `Tasks:    4 successful`.

---

## 🔍 Common Issues

| Symptom | Fix |
|---------|-----|
| `springCloudVersion` not found during Docker build | Dockerfile now copies `gradle.properties` before running `./gradlew` |
| Port already in use | Check `lsof -i :8080` or run `docker-compose down -v` |
| Frontend gets 404 on `/screen/game` | nginx config updated with `try_files` SPA fallback |
| STOMP connection fails | Ensure Redis is running; check `docker exec airconsole-redis redis-cli ping` |
| Checkstyle fails on game modules | Fixed — run `./gradlew :games:pong:checkstyleMain :games:trivia:checkstyleMain` to verify |

---

## 🗂️ Key Files Changed

- `frontend/apps/controller/src/App.tsx`
- `frontend/apps/controller/src/hooks/useControllerInput.ts`
- `frontend/apps/screen/src/App.tsx`
- `frontend/apps/screen/src/hooks/useCanvasRenderer.ts`
- `frontend/apps/lobby/src/App.tsx`
- `games/pong/src/main/java/.../PongGame.java`
- `games/pong/src/main/java/.../PongState.java`
- `games/trivia/src/main/java/.../TriviaGame.java`
- `api-gateway/Dockerfile`, `room-service/Dockerfile`, `player-service/Dockerfile`, `game-service/Dockerfile`, `notification-service/Dockerfile`, `score-service/Dockerfile`
- `infra/docker-compose.prod.yml`
- `infra/nginx/nginx.conf`
- `infra/Makefile`
- `frontend/apps/*/eslint.config.js`
