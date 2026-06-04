// Types for game state snapshots from Java backend

export interface Point {
  x: number;
  y: number;
}

export interface SnakeBody {
  id: string;
  alive: boolean;
  color: string;
  body: Point[];
}

export interface SnakeSnapshot {
  width: number;
  height: number;
  food: Point;
  snakes: SnakeBody[];
  scores: Record<string, number>;
  tick: number;
}

export interface PongSnapshot {
  width: number;
  height: number;
  ballX: number;
  ballY: number;
  ballVX: number;
  ballVY: number;
  leftPaddleY: number;
  rightPaddleY: number;
  paddleHeight: number;
  scores: Record<string, number>;
  tick: number;
  winScore: number;
}

export interface TriviaQuestion {
  question: string;
  optionA: string;
  optionB: string;
  optionC: string;
  optionD: string;
}

export type TriviaPhase = 'QUESTION' | 'TIMEOUT' | 'ROUND_END' | 'GAME_OVER';
export type GameStatus = 'WAITING' | 'RUNNING' | 'FINISHED';

export interface TriviaSnapshot {
  gameStatus: GameStatus;
  tick: number;
  currentRound: number;
  totalRounds: number;
  tickInRound: number;
  timeRemaining: number;
  phase: TriviaPhase;
  currentQuestion?: TriviaQuestion;
  correctAnswer?: string;
  scores: Record<string, number>;
  winnerId?: string;
  isTie?: boolean;
}

export type GameType = 'SNAKE' | 'PONG' | 'TRIVIA';

export interface GameState {
  gameType: GameType;
  payload: SnakeSnapshot | PongSnapshot | TriviaSnapshot;
}