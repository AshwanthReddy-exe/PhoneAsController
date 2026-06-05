// Controller app calls POST /api/players/register and GET /api/games/{gameType}/layout
import { useEffect, useState, useCallback } from 'react';
import { Users, ArrowRight } from 'lucide-react';
import './index.css';

import { getStompClient, startGameApi, getPlayersApi, joinRoomApi } from '@airconsole/shared';
import DynamicController from './components/DynamicController';
import { useControllerInput } from './hooks/useControllerInput';

interface PlayerInfo {
  playerId: string;
  playerName?: string;
  nickname?: string;
  score: number;
}

function App() {
  const [roomCode, setRoomCode] = useState('');
  const [nickname, setNickname] = useState('');

  const [joined, setJoined] = useState(false);
  const [gameStarted, setGameStarted] = useState(false);
  const [gameType, setGameType] = useState('SNAKE');

  const [playerId, setPlayerId] = useState('');
  const [roomId, setRoomId] = useState('');
  const [, setToken] = useState('');
  const [isHost, setIsHost] = useState(false);

  // Restore session from URL params on initial mount
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const rCode = params.get('roomCode') || '';
    const rId = params.get('roomId') || '';
    const pId = params.get('playerId') || '';
    const pToken = params.get('token') || '';

    if (rCode && rId && pId && pToken) {
      setRoomCode(rCode);
      setRoomId(rId);
      setPlayerId(pId);
      setToken(pToken);
      localStorage.setItem('airconsole_token', pToken);
      setJoined(true);
      // Handled outside this effect to avoid set-state-in-effect lint
      void restoreSession(rId, pId);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const checkHostStatus = useCallback(async (rId: string, pId: string) => {
    try {
      const players: PlayerInfo[] = await getPlayersApi(rId);
      if (players.length > 0 && players[0].playerId === pId) {
        setIsHost(true);
      }
    } catch (e) {
      console.error('Failed to fetch players', e);
    }
  }, []);

  const connectStomp = useCallback((rId: string) => {
    const client = getStompClient();
    client.onConnect = () => {
      console.log('Connected to STOMP as controller!');
      client.subscribe(`/topic/room:${rId}`, (msg) => {
        try {
          const envelope = JSON.parse(msg.body);
          if (envelope.eventType === 'GameStartedEvent') {
            setGameStarted(true);
            if (envelope.payload?.gameType) {
              setGameType(envelope.payload.gameType);
            }
          } else if (envelope.eventType === 'GameStateUpdatedEvent') {
            setGameStarted(true);
          }
        } catch (e) {
          console.error(e);
        }
      });
    };
    client.activate();
  }, []);

  const restoreSession = useCallback(async (rId: string, pId: string) => {
    await checkHostStatus(rId, pId);
    connectStomp(rId);
  }, [checkHostStatus, connectStomp]);

  const handleJoin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!roomCode || !nickname) return;

    try {
      const { player, roomId: realRoomId } = await joinRoomApi(roomCode.toUpperCase(), nickname);
      setRoomId(realRoomId);
      setPlayerId(player.playerId);
      setToken(player.token);
      localStorage.setItem('airconsole_token', player.token);

      // Update URL to persist on refresh
      window.history.replaceState(
        {},
        '',
        `?roomCode=${roomCode.toUpperCase()}&roomId=${realRoomId}&playerId=${player.playerId}&token=${player.token}`
      );

      setJoined(true);
      await checkHostStatus(realRoomId, player.playerId);
      connectStomp(realRoomId);
    } catch (err) {
      console.error(err);
      alert('Failed to join room. Check the code.');
    }
  };

  const handleStartGame = async () => {
    if (!roomId) return;
    try {
      const players: PlayerInfo[] = await getPlayersApi(roomId);
      const pIds = players.map((p) => p.playerId);
      await startGameApi(roomId, 'SNAKE', pIds);
      setGameStarted(true);
    } catch (e) {
      console.error('Failed to start game', e);
    }
  };

  const { sendInput } = useControllerInput(playerId, roomId);

  const handleAction = (action: number) => {
    if (!playerId || !roomId) return;
    sendInput(action);
  };

  if (!joined) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center p-6 relative">
        <div className="absolute inset-0 bg-gradient-to-br from-primary/10 to-secondary/10 pointer-events-none" />
        <div className="w-full max-w-sm glass-panel neon-border-secondary rounded-3xl p-8 relative z-10">
          <div className="text-center mb-8">
            <h1 className="font-display font-extrabold text-4xl text-white mb-2">Connect</h1>
            <p className="text-white/60 text-sm font-sans">Look at the TV for the code</p>
          </div>

          <form onSubmit={handleJoin} className="space-y-6">
            <div>
              <label className="block text-white/50 text-xs font-bold mb-2 uppercase tracking-wider">
                Room Code
              </label>
              <input
                type="text"
                value={roomCode}
                onChange={(e) => setRoomCode(e.target.value.toUpperCase())}
                maxLength={5}
                className="w-full bg-surface border border-white/10 rounded-xl py-4 px-4 text-white text-center font-display font-bold tracking-[0.5em] text-2xl focus:outline-none focus:border-secondary focus:ring-1 focus:ring-secondary transition-all"
              />
            </div>

            <div>
              <label className="block text-white/50 text-xs font-bold mb-2 uppercase tracking-wider">
                Nickname
              </label>
              <input
                type="text"
                value={nickname}
                onChange={(e) => setNickname(e.target.value)}
                maxLength={12}
                placeholder="Spiderman"
                className="w-full bg-surface border border-white/10 rounded-xl py-4 px-4 text-white font-sans font-bold text-lg focus:outline-none focus:border-secondary focus:ring-1 focus:ring-secondary transition-all"
              />
            </div>

            <button
              type="submit"
              disabled={roomCode.length !== 5 || nickname.length < 2}
              className="w-full bg-secondary hover:bg-secondary/90 disabled:opacity-50 text-white font-display font-bold py-4 rounded-xl flex items-center justify-center gap-2 transition-all shadow-[0_0_20px_rgba(255,0,229,0.4)]"
            >
              <span>JOIN</span>
              <ArrowRight size={20} />
            </button>
          </form>
        </div>
      </div>
    );
  }

  if (!gameStarted) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center p-6">
        <div className="glass-panel w-full max-w-sm rounded-3xl p-8 text-center neon-border-primary flex flex-col h-96">
          <div className="flex-1 flex flex-col items-center justify-center">
            <Users size={48} className="text-primary mb-4 animate-pulse" />
            <h2 className="text-2xl font-display font-bold text-white mb-2">Connected!</h2>
            <p className="text-white/60 font-sans">Look at the TV screen.</p>
          </div>

          <div className="mt-auto">
            {isHost ? (
              <button
                onClick={handleStartGame}
                className="w-full bg-primary hover:bg-primary/90 text-black font-display font-bold py-5 rounded-2xl text-xl uppercase tracking-widest shadow-[0_0_20px_rgba(0,242,255,0.6)] transition-transform active:scale-95"
              >
                Start Game
              </button>
            ) : (
              <div className="p-4 rounded-xl bg-white/5 border border-white/10">
                <p className="text-white/80 font-sans">Waiting for Host to start...</p>
              </div>
            )}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="h-screen w-screen bg-background flex flex-col overflow-hidden fixed inset-0 touch-none select-none">
      <div className="bg-surface/80 backdrop-blur-md border-b border-white/10 px-6 py-4 flex justify-between items-center z-10">
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded-full bg-primary shadow-[0_0_10px_rgba(0,242,255,0.8)]" />
          <span className="text-white font-bold font-sans">Live</span>
        </div>
        <div className="px-3 py-1 bg-white/5 rounded-full border border-white/10 text-white/70 text-sm font-sans">
          {roomCode}
        </div>
      </div>

      <div className="flex-1 relative flex items-center justify-center p-4 sm:p-8">
        <div className="absolute inset-0 flex items-center justify-center opacity-[0.03] pointer-events-none">
          <div className="w-full aspect-square max-w-[80vw] rounded-full border-[20px] border-white" />
        </div>

        <DynamicController gameType={gameType} onInput={handleAction} />
      </div>
    </div>
  );
}

export default App;
