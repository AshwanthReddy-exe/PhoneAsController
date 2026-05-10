import { useEffect } from 'react';
import { ArrowLeft, ArrowRight } from 'lucide-react';

interface TwoButtonLayoutProps {
  onInput: (action: number) => void;
}

export default function TwoButtonLayout({ onInput }: TwoButtonLayoutProps) {
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      const map: Record<string, number> = {
        ArrowLeft: 3, ArrowRight: 4,
        a: 3, d: 4,
      };
      if (map[e.key] !== undefined) {
        e.preventDefault();
        onInput(map[e.key]);
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [onInput]);

  return (
    <div className="flex items-center justify-center h-full gap-8 p-8">
      <button
        onClick={() => onInput(3)}
        className="w-32 h-32 rounded-2xl bg-white/10 border border-white/20 flex items-center justify-center text-white active:bg-primary/30 active:border-primary transition-all"
      >
        <ArrowLeft size={48} />
      </button>
      <button
        onClick={() => onInput(4)}
        className="w-32 h-32 rounded-2xl bg-white/10 border border-white/20 flex items-center justify-center text-white active:bg-primary/30 active:border-primary transition-all"
      >
        <ArrowRight size={48} />
      </button>
    </div>
  );
}
