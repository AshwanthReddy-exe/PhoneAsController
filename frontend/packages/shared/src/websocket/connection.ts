import { Client } from '@stomp/stompjs';

// The API Gateway runs on 8080, but WebSockets should probably go directly to notification-service on 8085
// or we can route it through the gateway if we configured the gateway to proxy websockets.
// Let's connect directly to notification-service to be safe, or just use the relative URL if we proxy it via Vite.
// Let's use the vite proxy for /ws which we will need to add to vite configs.
export const getStompClient = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const host = window.location.host;
  const brokerURL = `${protocol}//${host}/ws`;

  const client = new Client({
    brokerURL,
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
  });

  return client;
};
