import { useRef, useEffect, useCallback, useState } from 'react';
import type { RefObject } from 'react';

interface UseCanvasOptions {
  onFrame?: (deltaTime: number) => void;
  autoStart?: boolean;
}

export function useCanvasRenderer(
  canvasRef: RefObject<HTMLCanvasElement | null>,
  options: UseCanvasOptions = {}
) {
  const { onFrame, autoStart = true } = options;
  const animationFrameRef = useRef<number | null>(null);
  const lastTimeRef = useRef<number>(0);
  const isRunningRef = useRef<boolean>(false);
  const [isRunning, setIsRunning] = useState(false);
  const loopRef = useRef<((timestamp: number) => void) | null>(null);

  const loop = useCallback((timestamp: number) => {
    if (!isRunningRef.current) return;

    const deltaTime = timestamp - lastTimeRef.current;
    lastTimeRef.current = timestamp;

    if (onFrame) {
      onFrame(deltaTime);
    }

    animationFrameRef.current = requestAnimationFrame(loopRef.current!);
  }, [onFrame]);

  // Store loop in ref so it can reference itself without TDZ issues
  useEffect(() => {
    loopRef.current = loop;
  }, [loop]);

  const start = useCallback(() => {
    if (isRunningRef.current) return;

    isRunningRef.current = true;
    setIsRunning(true);
    lastTimeRef.current = performance.now();
    animationFrameRef.current = requestAnimationFrame(loopRef.current!);
  }, []);

  const stop = useCallback(() => {
    isRunningRef.current = false;
    setIsRunning(false);
    if (animationFrameRef.current !== null) {
      cancelAnimationFrame(animationFrameRef.current);
      animationFrameRef.current = null;
    }
  }, []);

  const toggle = useCallback(() => {
    if (isRunningRef.current) {
      stop();
    } else {
      start();
    }
  }, [start, stop]);

  // Auto-start/stop based on canvas presence
  useEffect(() => {
    if (autoStart && canvasRef.current) {
      start();
    }

    return () => {
      stop();
    };
  }, [autoStart, start, stop, canvasRef]);

  // Handle canvas resize
  const setupCanvas = useCallback((
    canvas: HTMLCanvasElement,
    width: number,
    height: number
  ) => {
    canvas.width = width;
    canvas.height = height;
    return canvas;
  }, []);

  return {
    start,
    stop,
    toggle,
    isRunning,
    setupCanvas,
    animationFrameRef,
  };
}
