import { randomUUID } from 'crypto';

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function run() {
  const GATEWAY = 'http://localhost:8080';
  console.log('--- STARTING SIMULATION ---');

  try {
    // 1. Create Room
    console.log('1. Creating Room...');
    const hostId = randomUUID();
    const createRes = await fetch(`${GATEWAY}/api/rooms`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ hostId, gameType: 'SNAKE', maxPlayers: 4 })
    });
    if (!createRes.ok) throw new Error(`Create Room failed: ${createRes.status}`);
    const room = await createRes.json();
    console.log(`-> Room Created: ID=${room.roomId}, Code=${room.roomCode}`);

    // 2. Join Room
    console.log('2. Joining Room...');
    const joinRes = await fetch(`${GATEWAY}/api/rooms/join`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ roomCode: room.roomCode })
    });
    if (!joinRes.ok) throw new Error(`Join Room failed: ${joinRes.status}`);
    const joinedRoom = await joinRes.json();

    // 3. Register Player
    console.log('3. Registering Player...');
    const regRes = await fetch(`${GATEWAY}/api/players/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ roomId: room.roomId, playerName: 'SimPlayer1' })
    });
    if (!regRes.ok) throw new Error(`Register Player failed: ${regRes.status}`);
    const player = await regRes.json();
    console.log(`-> Player Registered: ID=${player.playerId}, Token=${player.token}`);

    // 4. Start Game
    console.log('4. Starting Game...');
    const startRes = await fetch(`${GATEWAY}/api/games/start?roomId=${room.roomId}&gameType=SNAKE`, {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${player.token}`
      },
      body: JSON.stringify([player.playerId])
    });
    
    if (!startRes.ok) {
        const errText = await startRes.text();
        throw new Error(`Start Game failed: ${startRes.status} ${errText}`);
    }
    const game = await startRes.json();
    console.log(`-> Game Started: ID=${game.gameId}`);

    // 5. Send Input
    console.log('5. Sending Input (UP)...');
    const inputRes = await fetch(`${GATEWAY}/api/games/input`, {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${player.token}`
      },
      body: JSON.stringify({
          roomId: room.roomId,
          playerId: player.playerId,
          actionCode: 1, // UP
          timestamp: Date.now()
      })
    });
    if (!inputRes.ok) {
        const errText = await inputRes.text();
        throw new Error(`Send Input failed: ${inputRes.status} ${errText}`);
    }
    console.log('-> Input Sent successfully.');

    console.log('--- SIMULATION COMPLETED SUCCESSFULLY ---');

  } catch (err) {
    console.error('SIMULATION ERROR:', err.message);
  }
}

run();
