import { useEffect, useRef } from 'react';
import type { PongSnapshot } from '../types';

interface PongRendererProps {
  snapshot: PongSnapshot;
  width: number;
  height: number;
}

const PADDLE_COLOR = '#FFFFFF';
const BALL_COLOR = '#FFFFFF';
const COURT_COLOR = '#1a1a2e';
const LINE_COLOR = '#3d3d5c';
const SCORE_COLOR = '#FFFFFF88';

export function PongRenderer({ snapshot, width, height }: PongRendererProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Calculate scale to fit game coordinates to canvas
    const scaleX = width / snapshot.width;
    const scaleY = height / snapshot.height;
    const scale = Math.min(scaleX, scaleY);
    
    // Center offset
    const offsetX = (width - snapshot.width * scale) / 2;
    const offsetY = (height - snapshot.height * scale) / 2;

    // Clear canvas
    ctx.fillStyle = COURT_COLOR;
    ctx.fillRect(0, 0, width, height);

    // Save context and apply transform
    ctx.save();
    ctx.translate(offsetX, offsetY);
    ctx.scale(scale, scale);

    // Draw center line
    ctx.strokeStyle = LINE_COLOR;
    ctx.lineWidth = 0.3;
    ctx.setLineDash([2, 2]);
    ctx.beginPath();
    ctx.moveTo(snapshot.width / 2, 0);
    ctx.lineTo(snapshot.width / 2, snapshot.height);
    ctx.stroke();
    ctx.setLineDash([]);

    // Draw court border
    ctx.strokeStyle = LINE_COLOR;
    ctx.lineWidth = 0.5;
    ctx.strokeRect(0, 0, snapshot.width, snapshot.height);

    // Draw paddles
    drawPaddle(ctx, 2, snapshot.leftPaddleY, snapshot.paddleHeight, PADDLE_COLOR);
    drawPaddle(ctx, snapshot.width - 2.5, snapshot.rightPaddleY, snapshot.paddleHeight, PADDLE_COLOR);

    // Draw ball
    drawBall(ctx, snapshot.ballX, snapshot.ballY, 0.5, BALL_COLOR);

    // Restore context for UI elements
    ctx.restore();

    // Draw scores (in canvas space, not game space)
    drawScores(ctx, snapshot.scores, width, height, snapshot.winScore);

    // Draw "First to X" indicator
    ctx.font = `bold ${height * 0.03}px monospace`;
    ctx.fillStyle = SCORE_COLOR;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'top';
    ctx.fillText(`First to ${snapshot.winScore}`, width / 2, 10);

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

function drawPaddle(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  height: number,
  color: string
) {
  const paddleWidth = 1.0;
  
  ctx.fillStyle = color;
  ctx.beginPath();
  ctx.roundRect(x, y, paddleWidth, height, 2);
  ctx.fill();

  // Add subtle gradient effect
  const gradient = ctx.createLinearGradient(x, y, x + paddleWidth, y);
  gradient.addColorStop(0, 'rgba(255,255,255,0.3)');
  gradient.addColorStop(1, 'rgba(255,255,255,0)');
  ctx.fillStyle = gradient;
  ctx.beginPath();
  ctx.roundRect(x, y, paddleWidth, height, 2);
  ctx.fill();
}

function drawBall(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  radius: number,
  color: string
) {
  // Glow effect
  ctx.shadowColor = color;
  ctx.shadowBlur = 10;

  ctx.fillStyle = color;
  ctx.beginPath();
  ctx.arc(x, y, radius, 0, Math.PI * 2);
  ctx.fill();

  // Reset shadow
  ctx.shadowBlur = 0;

  // Inner highlight
  const gradient = ctx.createRadialGradient(
    x - radius * 0.3,
    y - radius * 0.3,
    0,
    x,
    y,
    radius
  );
  gradient.addColorStop(0, 'rgba(255,255,255,0.8)');
  gradient.addColorStop(1, 'rgba(255,255,255,0)');
  ctx.fillStyle = gradient;
  ctx.beginPath();
  ctx.arc(x, y, radius, 0, Math.PI * 2);
  ctx.fill();
}

function drawScores(
  ctx: CanvasRenderingContext2D,
  scores: Record<string, number>,
  canvasWidth: number,
  canvasHeight: number,
  winScore: number
) {
  const entries = Object.values(scores);
  const leftScore = entries[0] ?? 0;
  const rightScore = entries[1] ?? 0;

  ctx.font = `bold ${canvasHeight * 0.15}px monospace`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';

  // Left score
  const leftColor = leftScore >= winScore ? '#00FF66' : '#FFFFFF';
  ctx.fillStyle = leftColor;
  ctx.fillText(leftScore.toString(), canvasWidth * 0.25, canvasHeight * 0.15);

  // Right score
  const rightColor = rightScore >= winScore ? '#00FF66' : '#FFFFFF';
  ctx.fillStyle = rightColor;
  ctx.fillText(rightScore.toString(), canvasWidth * 0.75, canvasHeight * 0.15);
}