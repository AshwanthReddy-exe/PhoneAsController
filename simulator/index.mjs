import axios from 'axios';
import { randomUUID } from 'crypto';

const GATEWAY_URL = process.env.GATEWAY_URL || 'http://localhost:8080';
const NUM_BOTS = parseInt(process.env.NUM_BOTS || '4', 10);
const ROOM_CODE = process.env.ROOM_CODE;

async function sleep(ms) {
    return new Promise(r => setTimeout(r, ms));
}

async function run() {
    console.log(`🤖 AirConsole Bot Simulator starting up...`);
    console.log(`Targeting: ${GATEWAY_URL}`);
    console.log(`Number of Bots: ${NUM_BOTS}`);

    let room;

    if (ROOM_CODE) {
        console.log(`Joining existing room: ${ROOM_CODE}`);
        try {
            const res = await axios.get(`${GATEWAY_URL}/api/rooms/${ROOM_CODE}`);
            room = res.data;
        } catch (e) {
            console.error('Failed to fetch room:', e.response?.data || e.message);
            return;
        }
    } else {
        console.log(`Creating a new room...`);
        try {
            const res = await axios.post(`${GATEWAY_URL}/api/rooms`, {
                hostId: randomUUID(),
                gameType: 'SNAKE',
                maxPlayers: NUM_BOTS
            });
            room = res.data;
            console.log(`✅ Created Room! Code: ${room.roomCode}`);
        } catch (e) {
            console.error('Failed to create room:', e.response?.data || e.message);
            return;
        }
    }

    console.log(`-------------------------------------`);
    console.log(`📺 OPEN YOUR BROWSER TO SEE THE BOTS!`);
    console.log(`👉 http://localhost:3002/?roomId=${room.roomId}&roomCode=${room.roomCode}`);
    console.log(`-------------------------------------`);

    const bots = [];

    // 1. Join Room & Register Bots
    for (let i = 0; i < NUM_BOTS; i++) {
        try {
            console.log(`Bot ${i} joining room...`);
            await axios.post(`${GATEWAY_URL}/api/rooms/join`, { roomCode: room.roomCode });

            console.log(`Bot ${i} registering...`);
            const regRes = await axios.post(`${GATEWAY_URL}/api/players/register`, {
                roomId: room.roomId,
                playerName: `Bot_${i + 1}`
            });
            bots.push(regRes.data);
            await sleep(200); // slight stagger
        } catch (e) {
            console.error(`Bot ${i} failed:`, e.response?.data || e.message);
        }
    }

    console.log(`✅ All ${bots.length} bots joined!`);

    if (!ROOM_CODE) {
        console.log(`Waiting 5 seconds before starting game so you can open the browser...`);
        await sleep(5000);

        try {
            const playerIds = bots.map(b => b.playerId);
            console.log(`Starting game with players:`, playerIds);
            
            // Only host can start but in our current mock any authenticated player can hit the endpoint if they pass playerIds
            const startRes = await axios.post(`${GATEWAY_URL}/api/games/start?roomId=${room.roomId}&gameType=SNAKE`, playerIds, {
                headers: { 'Authorization': `Bearer ${bots[0].token}` }
            });
            console.log(`✅ Game Started! ID: ${startRes.data.gameId}`);
        } catch (e) {
            console.error('Failed to start game:', e.response?.data || e.message);
            return;
        }
    }

    // 2. Play the game! Every 200-800ms, a bot will change direction randomly
    console.log(`🎮 Bots are now playing... Press Ctrl+C to stop.`);
    
    // Actions: 1=UP, 2=DOWN, 3=LEFT, 4=RIGHT
    const validActions = [1, 2, 3, 4];

    const intervals = bots.map(bot => {
        return setInterval(async () => {
            const action = validActions[Math.floor(Math.random() * validActions.length)];
            try {
                await axios.post(`${GATEWAY_URL}/api/games/input`, {
                    roomId: room.roomId,
                    playerId: bot.playerId,
                    actionCode: action,
                    timestamp: Date.now()
                }, {
                    headers: { 'Authorization': `Bearer ${bot.token}` }
                });
            } catch (e) {
                // Ignore 400s which happen if the game is over or player is dead
                if (e.response?.status !== 400) {
                    console.error(`Bot ${bot.playerId} input failed:`, e.message);
                }
            }
        }, Math.random() * 600 + 400); // Random interval between 400ms and 1000ms
    });

    process.on('SIGINT', () => {
        console.log('Shutting down bots...');
        intervals.forEach(clearInterval);
        process.exit();
    });
}

run();
