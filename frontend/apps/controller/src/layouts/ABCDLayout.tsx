import { useEffect } from 'react';

interface ABCDLayoutProps {
  onInput: (action: number) => void;
}

export default function ABCDLayout({ onInput }: ABCDLayoutProps) {
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      const map: Record<string, number> = {
        a: 5, b: 6, c: 7, d: 8,
        A: 5, B: 6, C: 7, D: 8,
        1: 5, 2: 6, 3: 7, 4: 8,
      };
      if (map[e.key] !== undefined) {
        e.preventDefault();
        onInput(map[e.key]);
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [onInput]);

  const buttons = [
    { label: 'A', action: 5, color: 'bg-red-500' },
    { label: 'B', action: 6, color: 'bg-blue-500' },
    { label: 'C', action: 7, color: 'bg-green-500' },
    { label: 'D', action: 8, color: 'bg-yellow-500' },
  ];

  return (
    <div className="grid grid-cols-2 gap-4 p-8 h-full">
      {buttons.map((btn) => (
        <button
          key={btn.label}
          onClick={() => onInput(btn.action)}
          className={`${btn.color} rounded-2xl text-white font-bold text-3xl flex items-center justify-center active:opacity-80 transition-opacity`}
        >
          {btn.label}
        </button>
      ))}
    </div>
  );
}
