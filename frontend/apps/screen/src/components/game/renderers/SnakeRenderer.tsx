import { useEffect, useRef } from 'react';
import type { SnakeSnapshot, Point } from '../types';

interface SnakeRendererProps {
  snapshot: SnakeSnapshot;
  width: number;
  height: number;
}

// Snake colors from Java backend
const SNAKE_COLORS = ['#FF0055', '#00FF66', '#00CCFF', '#FFFF00', '#FF9900', '#FF00FF'];
const FOOD_COLOR = '#FFFFFF';
const GRID_COLOR = '#1a1a2e';
const GRID_LINE_COLOR = '#2d2d44';

export function SnakeRenderer({ snapshot, width, height }: SnakeRendererProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const cellWidth = width / snapshot.width;
    const cellHeight = height / snapshot.height;
    const cellSize = Math.min(cellWidth, cellHeight);

    // Clear canvas
    ctx.fillStyle = GRID_COLOR;
    ctx.fillRect(0, 0, width, height);

    // Draw grid lines
    ctx.strokeStyle = GRID_LINE_COLOR;
    ctx.lineWidth = 0.5;
    for (let x = 0; x <= snapshot.width; x++) {
      ctx.beginPath();
      ctx.moveTo(x * cellSize, 0);
      ctx.lineTo(x * cellSize, snapshot.height * cellSize);
      ctx.stroke();
    }
    for (let y = 0; y <= snapshot.height; y++) {
      ctx.beginPath();
      ctx.moveTo(0, y * cellSize);
      ctx.lineTo(snapshot.width * cellSize, y * cellSize);
      ctx.stroke();
    }

    // Draw food
    if (snapshot.food) {
      drawFood(ctx, snapshot.food, cellSize, FOOD_COLOR);
    }

    // Draw snakes
    for (const snake of snapshot.snakes) {
      drawSnake(ctx, snake, cellSize, snake.color);
    }

    // Draw scores in top corners
    drawScores(ctx, snapshot.scores, width, height, snapshot.snakes, cellSize);

  }, [snapshot, width, height]);

  return (
    <canvas
      ref={canvasRef}
      width={width}
      height={height}
      style={{ display: 'block' }}
    />
  );
}

function drawFood(ctx: CanvasRenderingContext2D, food: Point, cellSize: number, color: string) {
  const x = food.x * cellSize;
  const y = food.y * cellSize;
  const padding = cellSize * 0.1;
  const size = cellSize - padding * 2;

  ctx.fillStyle = color;
  ctx.beginPath();
  ctx.arc(
    x + cellSize / 2,
    y + cellSize / 2,
    size / 2,
    0,
    Math.PI * 2
  );
  ctx.fill();
}

function drawSnake(
  ctx: CanvasRenderingContext2D,
  snake: { id: string; alive: boolean; color: string; body: Point[] },
  cellSize: number,
  color: string
) {
  const padding = cellSize * 0.05;
  const size = cellSize - padding * 2;

  ctx.fillStyle = color;
  
  // Make dead snakes semi-transparent
  if (!snake.alive) {
    ctx.globalAlpha = 0.4;
  }

  // Draw body segments
  for (let i = snake.body.length - 1; i >= 0; i--) {
    const segment = snake.body[i];
    const x = segment.x * cellSize + padding;
    const y = segment.y * cellSize + padding;

    if (i === 0) {
      // Draw head with slightly different style
      ctx.fillStyle = lightenColor(color, 30);
      ctx.beginPath();
      ctx.roundRect(x, y, size + padding * 2, size + padding * 2, 4);
      ctx.fill();
      
      // Draw eyes
      ctx.fillStyle = '#000';
      ctx.beginPath();
      ctx.arc(x + size * 0.3, y + size * 0.3, size * 0.15, 0, Math.PI * 2);
      ctx.arc(x + size * 0.7, y + size * 0.3, size * 0.15, 0, Math.PI * 2);
      ctx.fill();
    } else {
      ctx.fillStyle = color;
      ctx.beginPath();
      ctx.roundRect(x, y, size + padding * 2, size + padding * 2, 3);
      ctx.fill();
    }
  }

  ctx.globalAlpha = 1.0;
  ctx.fillStyle = color;
}

function lightenColor(hex: string, percent: number): string {
  const num = parseInt(hex.replace('#', ''), 16);
  const amt = Math.round(2.55 * percent);
  const R = Math.min(255, (num >> 16) + amt);
  const G = Math.min(255, ((num >> 8) & 0x00FF) + amt);
  const B = Math.min(255, (num & 0x0000FF) + amt);
  return `#${(1 << 24 | R << 16 | G << 8 | B).toString(16).slice(1)}`;
}

function drawScores(
  ctx: CanvasRenderingContext2D,
  scores: Record<string, number>,
  canvasWidth: number,
  _canvasHeight: number,
  snakes: { id: string; color: string }[],
  cellSize: number
) {
  ctx.font = `bold ${Math.floor(cellSize * 1.5)}px monospace`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'top';

  const snakeColors: Record<string, string> = {};
  snakes.forEach(s => { snakeColors[s.id] = s.color; });

  let index = 0;
  for (const [id, score] of Object.entries(scores)) {
    const color = snakeColors[id] || SNAKE_COLORS[index % SNAKE_COLORS.length];
    const x = index === 0 ? cellSize * 2 : canvasWidth - cellSize * 2;
    
    ctx.fillStyle = color;
    ctx.fillText(`P${index + 1}: ${score}`, x, cellSize);
    index++;
  }
}