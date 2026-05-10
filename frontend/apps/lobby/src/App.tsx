import React from 'react';
import { BrowserRouter, Routes, Route, useNavigate } from 'react-router-dom';
import { Gamepad2, Zap, Users } from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';
import { createRoomApi } from '@airconsole/shared';
import './index.css';

const GAMES = [
  {
    id: 'SNAKE',
    name: 'Neon Snake',
    description: 'Classic snake, reimagined in a cyberpunk world. 1-4 Players.',
    maxPlayers: 4,
    color: 'primary',
    icon: <Gamepad2 size={48} className="text-primary mb-4" />
  },
  {
    id: 'PONG',
    name: 'Plasma Pong',
    description: 'Coming soon... Deflect the plasma orb.',
    maxPlayers: 2,
    color: 'secondary',
    icon: <Zap size={48} className="text-secondary mb-4" />,
    disabled: true
  },
  {
    id: 'TRIVIA',
    name: 'Cyber Trivia',
    description: 'Coming soon... Test your knowledge.',
    maxPlayers: 8,
    color: 'accent',
    icon: <Users size={48} className="text-accent mb-4" />,
    disabled: true
  }
];

function HomePage() {
  const handleSelectGame = async (gameId: string, maxPlayers: number) => {
    try {
      const room = await createRoomApi(crypto.randomUUID(), gameId, maxPlayers);
      window.location.assign(`http://localhost:3002/?roomCode=${room.roomCode}&roomId=${room.roomId}&isHost=true`);
    } catch (err) {
      console.error(err);
      alert('Failed to launch game session.');
    }
  };

  return (
    <div className="min-h-screen bg-background relative p-8">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-1/4 left-1/4 w-[500px] h-[500px] bg-primary/10 rounded-full blur-[120px]" />
        <div className="absolute bottom-1/4 right-1/4 w-[500px] h-[500px] bg-secondary/10 rounded-full blur-[120px]" />
      </div>

      <div className="relative z-10 max-w-6xl mx-auto flex flex-col items-center">
        <header className="mb-16 text-center mt-12">
          <h1 className="font-display font-extrabold text-6xl md:text-8xl tracking-tighter text-white mb-4">
            Air<span className="neon-text-primary">Console</span>
          </h1>
          <p className="text-white/70 font-sans text-xl md:text-2xl max-w-2xl mx-auto">
            Your phone is the controller. The screen is the world. Select a game to start playing.
          </p>
        </header>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8 w-full">
          {GAMES.map((game) => (
            <button
              key={game.id}
              disabled={game.disabled}
              onClick={() => handleSelectGame(game.id, game.maxPlayers)}
              className={`
                group relative glass-panel rounded-2xl p-8 text-left transition-all duration-300
                ${game.disabled ? 'opacity-50 cursor-not-allowed grayscale' : `hover:scale-[1.02] hover:-translate-y-2 active:scale-95 neon-border-${game.color}`}
              `}
            >
              <div className={`absolute inset-0 bg-${game.color}/5 rounded-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-300`} />
              <div className="relative z-10">
                {game.icon}
                <h2 className="font-display font-bold text-3xl text-white mb-2">{game.name}</h2>
                <p className="text-white/60 font-sans text-lg mb-6 line-clamp-2">
                  {game.description}
                </p>
                <div className={`inline-flex items-center px-4 py-2 rounded-full border border-${game.color}/30 bg-${game.color}/10 text-${game.color} font-bold text-sm`}>
                  {game.maxPlayers} Players Max
                </div>
              </div>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

function RoomPage() {
  const roomCode = new URLSearchParams(window.location.search).get('code') || '';

  return (
    <div className="min-h-screen bg-background relative p-8 flex flex-col items-center justify-center">
      <div className="glass-panel rounded-2xl p-12 text-center max-w-lg w-full">
        <h2 className="font-display font-bold text-4xl text-white mb-6">Room Ready!</h2>
        <div className="bg-white p-4 rounded-xl mb-6 flex items-center justify-center">
          <QRCodeSVG value={`${window.location.origin}/join?code=${roomCode}`} size={160} />
        </div>
        <p className="text-white/70 text-xl mb-2">Scan QR or enter code:</p>
        <p className="font-mono text-4xl text-primary font-bold tracking-widest mb-8">{roomCode}</p>
        <p className="text-white/50">Waiting for players to join...</p>
      </div>
    </div>
  );
}

function JoinPage() {
  const [code, setCode] = React.useState('');
  const navigate = useNavigate();

  const handleJoin = () => {
    if (code.trim()) {
      navigate(`/room/${code}`);
    }
  };

  return (
    <div className="min-h-screen bg-background relative p-8 flex flex-col items-center justify-center">
      <div className="glass-panel rounded-2xl p-12 text-center max-w-lg w-full">
        <h2 className="font-display font-bold text-4xl text-white mb-6">Join Room</h2>
        <input
          type="text"
          value={code}
          onChange={(e) => setCode(e.target.value.toUpperCase())}
          placeholder="Enter room code"
          className="w-full bg-black/30 border border-white/20 rounded-xl px-6 py-4 text-white text-xl mb-6 focus:outline-none focus:border-primary"
        />
        <button
          onClick={handleJoin}
          className="w-full bg-primary text-black font-bold text-xl py-4 rounded-xl hover:bg-primary/80 transition-colors"
        >
          Join Game
        </button>
      </div>
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/room/:roomId" element={<RoomPage />} />
        <Route path="/join" element={<JoinPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
