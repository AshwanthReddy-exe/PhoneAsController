import { useEffect } from 'react';
import { ArrowUp, ArrowDown, ArrowLeft, ArrowRight } from 'lucide-react';

interface DPadLayoutProps {
  onInput: (action: number) => void;
}

export default function DPadLayout({ onInput }: DPadLayoutProps) {
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      const map: Record<string, number> = {
        ArrowUp: 1, ArrowDown: 2, ArrowLeft: 3, ArrowRight: 4,
        w: 1, s: 2, a: 3, d: 4,
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
    { id: 'up', label: 'UP', action: 1, icon: <ArrowUp size={32} />, x: 1, y: 0 },
    { id: 'down', label: 'DOWN', action: 2, icon: <ArrowDown size={32} />, x: 1, y: 2 },
    { id: 'left', label: 'LEFT', action: 3, icon: <ArrowLeft size={32} />, x: 0, y: 1 },
    { id: 'right', label: 'RIGHT', action: 4, icon: <ArrowRight size={32} />, x: 2, y: 1 },
  ];

  return (
    <div className="flex flex-col items-center justify-center h-full gap-4 p-8">
      <div className="grid grid-cols-3 gap-3">
        {buttons.map((btn) => (
          <button
            key={btn.id}
            onClick={() => onInput(btn.action)}
            className={`
              w-24 h-24 rounded-2xl bg-white/10 border border-white/20 
              flex items-center justify-center text-white
              active:bg-primary/30 active:border-primary transition-all
              ${btn.x === 1 && btn.y === 0 ? 'col-start-2' : ''}
              ${btn.x === 0 && btn.y === 1 ? 'col-start-1' : ''}
              ${btn.x === 2 && btn.y === 1 ? 'col-start-3' : ''}
              ${btn.x === 1 && btn.y === 2 ? 'col-start-2' : ''}
            `}
          >
            {btn.icon}
          </button>
        ))}
      </div>
    </div>
  );
}
