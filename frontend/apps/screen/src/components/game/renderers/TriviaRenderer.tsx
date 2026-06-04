import { useEffect, useRef } from 'react';
import type { TriviaSnapshot } from '../types';

interface TriviaRendererProps {
  snapshot: TriviaSnapshot;
  width: number;
  height: number;
}

const COLORS = {
  background: '#1a1a2e',
  primary: '#FFFFFF',
  accent: '#00CCFF',
  correct: '#00FF66',
  wrong: '#FF0055',
  muted: '#888899',
  timerFull: '#00FF66',
  timerEmpty: '#FF0055',
};

const OPTION_COLORS = ['#FF0055', '#00CCFF', '#FFFF00', '#FF9900'];

export function TriviaRenderer({ snapshot, width, height }: TriviaRendererProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Clear canvas
    ctx.fillStyle = COLORS.background;
    ctx.fillRect(0, 0, width, height);

    switch (snapshot.phase) {
      case 'QUESTION':
        drawQuestionPhase(ctx, snapshot, width, height);
        break;
      case 'ROUND_END':
        drawRoundEndPhase(ctx, snapshot, width, height);
        break;
      case 'GAME_OVER':
        drawGameOverPhase(ctx, snapshot, width, height);
        break;
      default:
        drawWaitingState(ctx, width, height);
    }

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

function drawQuestionPhase(
  ctx: CanvasRenderingContext2D,
  snapshot: TriviaSnapshot,
  width: number,
  height: number
) {
  const question = snapshot.currentQuestion;
  if (!question) return;

  const padding = width * 0.05;
  const fontSize = Math.min(width, height) * 0.04;
  const optionFontSize = fontSize * 0.85;

  // Round indicator
  ctx.font = `bold ${fontSize}px monospace`;
  ctx.fillStyle = COLORS.accent;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'top';
  ctx.fillText(
    `Round ${snapshot.currentRound} / ${snapshot.totalRounds}`,
    width / 2,
    padding
  );

  // Timer bar
  const timerHeight = height * 0.02;
  const timerY = padding + fontSize + padding * 0.5;
  const timerWidth = width - padding * 2;
  const timerProgress = snapshot.timeRemaining / 10;
  const timerColor = timerProgress > 0.5 ? COLORS.timerFull : COLORS.timerEmpty;

  ctx.fillStyle = COLORS.muted;
  ctx.fillRect(padding, timerY, timerWidth, timerHeight);
  ctx.fillStyle = timerColor;
  ctx.fillRect(padding, timerY, timerWidth * timerProgress, timerHeight);

  // Question text
  const questionY = timerY + timerHeight + padding;
  ctx.font = `bold ${fontSize * 1.3}px monospace`;
  ctx.fillStyle = COLORS.primary;
  ctx.textAlign = 'center';

  // Word wrap for question
  const maxQuestionWidth = width - padding * 4;
  const wrappedLines = wrapText(ctx, question.question, maxQuestionWidth);
  wrappedLines.forEach((line, i) => {
    ctx.fillText(line, width / 2, questionY + i * fontSize * 1.5);
  });

  // Options
  const optionsY = questionY + wrappedLines.length * fontSize * 1.5 + padding * 2;
  const options = [
    { label: 'A', text: question.optionA },
    { label: 'B', text: question.optionB },
    { label: 'C', text: question.optionC },
    { label: 'D', text: question.optionD },
  ];

  options.forEach((opt, i) => {
    const y = optionsY + i * (optionFontSize * 2.5);
    drawOption(ctx, opt.label, opt.text, padding, y, width - padding * 2, optionFontSize, OPTION_COLORS[i]);
  });

  // Draw scores at bottom
  drawPlayerScores(ctx, snapshot.scores, width, height, padding);
}

function drawOption(
  ctx: CanvasRenderingContext2D,
  label: string,
  text: string,
  x: number,
  y: number,
  maxWidth: number,
  fontSize: number,
  color: string
) {
  const boxHeight = fontSize * 2;

  // Option box background
  ctx.fillStyle = color + '33'; // Semi-transparent
  ctx.beginPath();
  ctx.roundRect(x, y, maxWidth, boxHeight, 8);
  ctx.fill();

  // Option box border
  ctx.strokeStyle = color;
  ctx.lineWidth = 2;
  ctx.beginPath();
  ctx.roundRect(x, y, maxWidth, boxHeight, 8);
  ctx.stroke();

  // Option label (letter)
  ctx.font = `bold ${fontSize * 1.3}px monospace`;
  ctx.fillStyle = color;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText(label, x + boxHeight / 2, y + boxHeight / 2);

  // Option text
  ctx.font = `${fontSize}px monospace`;
  ctx.fillStyle = COLORS.primary;
  ctx.textAlign = 'left';
  const textX = x + boxHeight + fontSize;
  const wrappedLines = wrapText(ctx, text, maxWidth - boxHeight - fontSize * 2);
  wrappedLines.forEach((line, i) => {
    ctx.fillText(line, textX, y + (boxHeight / 2) + (i - (wrappedLines.length - 1) / 2) * fontSize * 1.2);
  });
}

function drawRoundEndPhase(
  ctx: CanvasRenderingContext2D,
  snapshot: TriviaSnapshot,
  width: number,
  height: number
) {
  const padding = width * 0.05;
  const fontSize = Math.min(width, height) * 0.04;
  const question = snapshot.currentQuestion;

  // Round end message
  ctx.font = `bold ${fontSize * 1.5}px monospace`;
  ctx.fillStyle = COLORS.accent;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText('Round Complete!', width / 2, height * 0.3);

  // Show the correct answer
  if (snapshot.correctAnswer && question) {
    const correctOption = ['A', 'B', 'C', 'D'].indexOf(snapshot.correctAnswer);
    const optionLabels = ['optionA', 'optionB', 'optionC', 'optionD'] as const;
    const correctText = question[optionLabels[correctOption]];

    ctx.font = `${fontSize}px monospace`;
    ctx.fillStyle = COLORS.primary;
    ctx.fillText(`Correct Answer: ${snapshot.correctAnswer}) ${correctText}`, width / 2, height * 0.45);
  }

  // Current scores
  ctx.font = `bold ${fontSize}px monospace`;
  ctx.fillStyle = COLORS.accent;
  ctx.fillText('Current Standings', width / 2, height * 0.6);

  drawPlayerScores(ctx, snapshot.scores, width, height * 0.85, padding);
}

function drawGameOverPhase(
  ctx: CanvasRenderingContext2D,
  snapshot: TriviaSnapshot,
  width: number,
  height: number
) {
  const padding = width * 0.05;
  const fontSize = Math.min(width, height) * 0.04;

  // Winner announcement
  ctx.font = `bold ${fontSize * 2}px monospace`;
  ctx.fillStyle = COLORS.correct;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';

  if (snapshot.isTie) {
    ctx.fillText("It's a Tie!", width / 2, height * 0.3);
  } else {
    ctx.fillText('Winner!', width / 2, height * 0.3);
  }

  // Final scores
  ctx.font = `bold ${fontSize}px monospace`;
  ctx.fillStyle = COLORS.accent;
  ctx.fillText('Final Scores', width / 2, height * 0.5);

  drawPlayerScores(ctx, snapshot.scores, width, height * 0.7, padding);

  // "Game Over" text
  ctx.font = `${fontSize * 0.8}px monospace`;
  ctx.fillStyle = COLORS.muted;
  ctx.fillText('Game Over - Thank you for playing!', width / 2, height * 0.92);
}

function drawWaitingState(ctx: CanvasRenderingContext2D, width: number, height: number) {
  const fontSize = Math.min(width, height) * 0.04;

  ctx.font = `bold ${fontSize * 1.5}px monospace`;
  ctx.fillStyle = COLORS.accent;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText('Waiting for trivia to start...', width / 2, height / 2);
}

function drawPlayerScores(
  ctx: CanvasRenderingContext2D,
  scores: Record<string, number>,
  width: number,
  height: number,
  padding: number
) {
  const entries = Object.entries(scores);
  if (entries.length === 0) return;

  const fontSize = Math.min(width, height) * 0.035;
  const scoreHeight = fontSize * 2.5;
  const totalWidth = width - padding * 2;
  const scoreWidth = Math.min(totalWidth / entries.length, width * 0.3);

  ctx.font = `bold ${fontSize}px monospace`;
  ctx.textAlign = 'center';

  entries.forEach(([, score], index) => {
    const x = padding + index * scoreWidth + scoreWidth / 2;
    const y = height - scoreHeight - padding;

    // Player box
    const boxColor = OPTION_COLORS[index % OPTION_COLORS.length];
    ctx.fillStyle = boxColor + '22';
    ctx.beginPath();
    ctx.roundRect(x - scoreWidth / 2 + padding / 2, y, scoreWidth - padding, scoreHeight, 8);
    ctx.fill();

    // Player label
    ctx.fillStyle = boxColor;
    ctx.fillText(`Player ${index + 1}`, x, y + fontSize);

    // Score
    ctx.font = `bold ${fontSize * 1.5}px monospace`;
    ctx.fillStyle = COLORS.primary;
    ctx.fillText(score.toString(), x, y + fontSize * 1.8);
    ctx.font = `bold ${fontSize}px monospace`;
  });
}

function wrapText(ctx: CanvasRenderingContext2D, text: string, maxWidth: number): string[] {
  const words = text.split(' ');
  const lines: string[] = [];
  let currentLine = '';

  for (const word of words) {
    const testLine = currentLine ? `${currentLine} ${word}` : word;
    const metrics = ctx.measureText(testLine);

    if (metrics.width > maxWidth && currentLine) {
      lines.push(currentLine);
      currentLine = word;
    } else {
      currentLine = testLine;
    }
  }

  if (currentLine) {
    lines.push(currentLine);
  }

  return lines.length > 0 ? lines : [''];
}