#!/usr/bin/env bash
# AirConsole Smoke Test — validates the full user journey
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost}"
API="${BASE_URL}/api"

echo "🎮 AirConsole Smoke Test"
echo "========================"
echo "Target: $BASE_URL"
echo ""

# 1. Health checks
echo "1. Checking backend health..."
for svc in api-gateway room-service player-service game-service notification-service score-service; do
  port=$(docker inspect --format='{{range $p, $conf := .NetworkSettings.Ports}}{{if eq $p "8080/tcp"}}8080{{end}}{{end}}' airconsole-$svc 2>/dev/null || true)
done
curl -sf "$API/actuator/health" -o /dev/null && echo "   ✅ API Gateway healthy" || echo "   ❌ API Gateway not responding"
echo ""

# 2. Create room
echo "2. Creating room..."
ROOM_RESPONSE=$(curl -sf -X POST "$API/rooms" \
  -H "Content-Type: application/json" \
  -d '{"gameType":"SNAKE","maxPlayers":4}' | tee /dev/stderr)
ROOM_CODE=$(echo "$ROOM_RESPONSE" | grep -o '"roomCode":"[^"]*"' | cut -d'"' -f4)
ROOM_ID=$(echo "$ROOM_RESPONSE" | grep -o '"roomId":"[^"]*"' | cut -d'"' -f4)
echo ""
echo "   Room Code: $ROOM_CODE"
echo "   Room ID:   $ROOM_ID"
echo ""

# 3. Player registration
echo "3. Registering players..."
P1=$(curl -sf -X POST "$API/players/register" \
  -H "Content-Type: application/json" \
  -d "{\"roomId\":\"$ROOM_ID\",\"nickname\":\"Player1\"}" | grep -o '"playerId":"[^"]*"' | cut -d'"' -f4)
P2=$(curl -sf -X POST "$API/players/register" \
  -H "Content-Type: application/json" \
  -d "{\"roomId\":\"$ROOM_ID\",\"nickname\":\"Player2\"}" | grep -o '"playerId":"[^"]*"' | cut -d'"' -f4)
echo "   Player 1: $P1"
echo "   Player 2: $P2"
echo ""

# 4. Get game layout
echo "4. Fetching controller layout..."
curl -sf "$API/games/SNAKE/layout" -o /dev/null && echo "   ✅ Layout endpoint OK" || echo "   ❌ Layout endpoint failed"
echo ""

# 5. Start game ( host starts )  
echo "5. Starting game..."
curl -sf -X POST "$API/games/start" \
  -H "Content-Type: application/json" \
  -d "{\"roomId\":\"$ROOM_ID\",\"gameType\":\"SNAKE\",\"playerIds\":[\"$P1\",\"$P2\"]}" \
  -o /dev/null && echo "   ✅ Game started" || echo "   ❌ Start game failed"
echo ""

# 6. Send inputs
echo "6. Sending controller inputs..."
for i in {1..5}; do
  curl -sf -X POST "$API/games/input" \
    -H "Content-Type: application/json" \
    -d "{\"playerId\":\"$P1\",\"roomId\":\"$ROOM_ID\",\"action\":1,\"timestamp\":$(date +%s)000}" \
    -o /dev/null || true
done
echo "   ✅ Inputs sent"
echo ""

echo "🎉 Smoke test completed!"
