import { WebSocket } from 'ws';
import { Client } from '@stomp/stompjs';

// Get roomId from command line
const roomId = process.argv[2];
if (!roomId) {
  console.error("Usage: node test-stomp.mjs <roomId>");
  process.exit(1);
}

Object.assign(global, { WebSocket });

const client = new Client({
  brokerURL: 'ws://localhost:8085/ws',
  onConnect: () => {
    console.log("Connected to STOMP!");
    client.subscribe(`/topic/room.${roomId}`, (message) => {
      const envelope = JSON.parse(message.body);
      if (envelope.eventType === 'GameStateUpdatedEvent') {
        const payloadStr = Buffer.from(envelope.payload.snapshot.payload, 'base64').toString('utf8');
        console.log("Game State:", payloadStr);
        process.exit(0);
      }
    });
  }
});

client.activate();
