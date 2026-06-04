import { useEffect, useState } from 'react';
import { Users, Loader2, Smartphone } from 'lucide-react';
import './index.css';

import { getStompClient, getPlayersApi, startGameApi } from '@airconsole/shared';
import { GameCanvas } from './components/game/GameCanvas';
import type { GameType, SnakeSnapshot, PongSnapshot, TriviaSnapshot } from './components/game/types';

interface Player {
  id: string;
  name: string;
  score: number;
}

function getInitialParams() {
  const params = new URLSearchParams(window.location.search);
  return {
    code: params.get('roomCode') || '',
    id: params.get('roomId') || '',
    hostFlag: params.get('isHost') === 'true',
  };
}

function App() {
  const [gameState, setGameState] = useState<SnakeSnapshot | PongSnapshot | TriviaSnapshot | null>(null);
  const [players, setPlayers] = useState<Player[]>([]);
  const initial = getInitialParams();
  const [roomCode] = useState<string>(initial.code);
  const [roomId] = useState<string>(initial.id);
  const [isHost] = useState(initial.hostFlag);
  const [gameStarted, setGameStarted] = useState(false);
  const [gameType, setGameType] = useState<GameType>('SNAKE');

  useEffect(() => {
    if (!roomId) return;

    // Fetch initial players
    getPlayersApi(roomId).then((playerList: Array<{ playerId: string; playerName?: string; nickname?: string }>) => {
      setPlayers(playerList.map((p) => ({ id: p.playerId, name: p.playerName || p.nickname || 'Player', score: 0 })));
    }).catch((err: unknown) => console.error("Failed to fetch initial players", err));

    const client = getStompClient();
    client.onConnect = () => {
      console.log("Connected to STOMP broker!");
      client.subscribe(`/topic/room:${roomId}`, (message) => {
        try {
          const envelope = JSON.parse(message.body);

          if (envelope.eventType === 'GameStartedEvent') {
            setGameStarted(true);
            if (envelope.payload?.gameType) {
              setGameType(envelope.payload.gameType as GameType);
            }
          } else if (envelope.eventType === 'GameStateUpdatedEvent' && envelope.payload && envelope.payload.snapshot && envelope.payload.snapshot.payload) {
            setGameStarted(true);
            const decodedPayload = atob(envelope.payload.snapshot.payload);
            const state = JSON.parse(decodedPayload);
            setGameState(state);
          } else if (envelope.eventType === 'PlayerJoinedEvent' || envelope.eventType === 'PlayerJoinedRoomEvent') {
            const p = envelope.payload;
            setPlayers(prev => {
              if (prev.find(player => player.id === p.playerId)) return prev;
              return [...prev, { id: p.playerId, name: p.nickname || p.playerName || 'Player', score: 0 }];
            });
          } else if (envelope.eventType === 'PlayerLeftEvent' || envelope.eventType === 'PlayerDisconnectedEvent') {
            const pId = envelope.payload.playerId;
            setPlayers(prev => prev.filter(player => player.id !== pId));
          }
        } catch (e) {
          console.error("Failed to parse game state message", e);
        }
      });
    };
    client.activate();
  }, [roomId]);



  if (!roomCode) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center text-white">
        <Loader2 className="w-12 h-12 text-primary animate-spin mb-4" />
        <h1 className="text-2xl font-display">Initializing Screen...</h1>
      </div>
    );
  }

  // --- WAITING ROOM UI ---
  if (!gameStarted) {
    return (
      <div className="min-h-screen bg-background relative flex flex-col items-center justify-center p-8">
        <div className="absolute inset-0 overflow-hidden pointer-events-none">
          <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-primary/20 rounded-full blur-[100px]" />
          <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-secondary/20 rounded-full blur-[100px]" />
        </div>

        <div className="relative z-10 w-full max-w-5xl bg-surface/50 backdrop-blur-md border border-white/10 rounded-3xl p-12 text-center shadow-2xl">
          <h2 className="text-white/60 font-sans text-2xl mb-4 tracking-widest uppercase font-bold">Join the Game</h2>
          <p className="text-white text-xl mb-8">Go to <span className="text-primary font-bold">localhost:3003</span> on your phone and enter this code:</p>
          
          <div className="flex justify-center mb-16">
            <div className="bg-black/50 border border-primary/50 shadow-[0_0_50px_rgba(0,242,255,0.3)] rounded-2xl px-16 py-8">
              <span className="font-display font-extrabold text-8xl tracking-[0.25em] text-white drop-shadow-[0_0_15px_rgba(255,255,255,0.5)]">
                {roomCode}
              </span>
            </div>
          </div>

          <div className="text-left">
            <div className="flex items-center gap-3 mb-6 border-b border-white/10 pb-4">
              <Users className="text-secondary" size={28} />
              <h3 className="text-2xl font-display font-bold text-white">Connected Players ({players.length})</h3>
            </div>
            
            {players.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12 opacity-50">
                <Smartphone className="w-16 h-16 text-white mb-4 animate-pulse" />
                <p className="text-white text-lg font-sans">Waiting for players to connect...</p>
              </div>
            ) : (
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                {players.map((p, idx) => (
                  <div key={idx} className="glass-panel rounded-xl p-4 flex items-center justify-center gap-3 border border-white/5 bg-white/5">
                    <div className="w-10 h-10 rounded-full bg-primary/20 flex items-center justify-center font-bold text-primary">
                      {p.name.charAt(0).toUpperCase()}
                    </div>
                    <span className="text-white font-bold font-sans truncate">{p.name}</span>
                  </div>
                ))}
              </div>
            )}
            
            {isHost && players.length > 0 && (
              <div className="mt-12 text-center">
                <button
                  onClick={() => startGameApi(roomId, 'SNAKE', players.map(p => p.id))}
                  className="bg-primary text-black font-bold text-xl py-4 px-12 rounded-xl hover:bg-primary/80 transition-colors shadow-[0_0_30px_rgba(0,242,255,0.4)]"
                >
                  Start Game
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    );
  }

  // --- ACTIVE GAME UI ---
  return (
    <div className="min-h-screen bg-background relative overflow-hidden flex">
      <div className="flex-1 flex items-center justify-center p-8">
        <div className="relative glass-panel rounded-2xl p-2 neon-border-primary">
          <GameCanvas gameType={gameType} snapshot={gameState} width={800} height={800} />
        </div>
      </div>

      <div className="w-80 bg-surface/50 border-l border-white/10 p-6 flex flex-col">
        <div className="flex items-center gap-3 mb-8">
          <Users className="text-primary" />
          <h2 className="font-display font-bold text-2xl text-white">Players</h2>
        </div>

        <div className="space-y-4 flex-1">
          {players.map((player) => {
            const score = gameState?.scores?.[player.id] || 0;
            return (
              <div key={player.id} className="glass-panel rounded-xl p-4 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-3 h-3 rounded-full bg-primary shadow-[0_0_10px_rgba(0,242,255,0.5)]" />
                  <span className="font-bold text-white font-sans">{player.name}</span>
                </div>
                <span className="font-display font-bold text-2xl text-secondary">{score}</span>
              </div>
            );
          })}
        </div>

        <div className="mt-auto glass-panel rounded-xl p-4 text-center">
          <p className="text-white/50 text-sm mb-1">Room Code</p>
          <p className="font-display font-bold text-xl tracking-widest text-white">{roomCode}</p>
        </div>
      </div>
    </div>
  );
}

export default App;
