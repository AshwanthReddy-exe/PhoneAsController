import { memo } from 'react';
import type { SnakeSnapshot, PongSnapshot, TriviaSnapshot, GameType } from './types';
import { SnakeRenderer } from './renderers/SnakeRenderer';
import { PongRenderer } from './renderers/PongRenderer';
import { TriviaRenderer } from './renderers/TriviaRenderer';

interface GameCanvasProps {
  gameType: GameType;
  snapshot: SnakeSnapshot | PongSnapshot | TriviaSnapshot | null;
  width?: number;
  height?: number;
}

export const GameCanvas = memo(function GameCanvas({
  gameType,
  snapshot,
  width = 800,
  height = 600,
}: GameCanvasProps) {
  if (!snapshot) {
    return (
      <div
        className="flex items-center justify-center"
        style={{ width, height, backgroundColor: '#0a0a0a' }}
      >
        <span className="text-white/50 text-xl font-mono">Waiting for game state...</span>
      </div>
    );
  }

  switch (gameType) {
    case 'SNAKE':
      return <SnakeRenderer snapshot={snapshot as SnakeSnapshot} width={width} height={height} />;
    case 'PONG':
      return <PongRenderer snapshot={snapshot as PongSnapshot} width={width} height={height} />;
    case 'TRIVIA':
      return <TriviaRenderer snapshot={snapshot as TriviaSnapshot} width={width} height={height} />;
    default:
      return (
        <div
          className="flex items-center justify-center text-red-400 font-mono"
          style={{ width, height, backgroundColor: '#0a0a0a' }}
        >
          Unknown game type: {gameType}
        </div>
      );
  }
});
