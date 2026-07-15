import { useEffect, useState } from 'react';

const DICE_FACES: Record<number, Record<number, string>> = {
  4: { 1: '⚀', 2: '⚁', 3: '⚂', 4: '⚃' },
  6: { 1: '⚀', 2: '⚁', 3: '⚂', 4: '⚃', 5: '⚄', 6: '⚅' },
};

interface SingleDieProps {
  sides: number;
  target: number;
  delay: number;
}

function SingleDie({ sides, target, delay }: SingleDieProps) {
  const [current, setCurrent] = useState(0);
  const face = DICE_FACES[sides]?.[current] ?? String(current);

  useEffect(() => {
    // Shuffle for ~1s, then settle
    const shuffle = setInterval(() => {
      setCurrent(Math.floor(Math.random() * sides) + 1);
    }, 60);
    const stop = setTimeout(() => {
      clearInterval(shuffle);
      setCurrent(target);
    }, 300 + delay);
    return () => {
      clearInterval(shuffle);
      clearTimeout(stop);
    };
  }, [sides, target, delay]);

  return (
    <div
      className="flex h-14 w-14 items-center justify-center rounded-lg border-2 border-bg-elevated bg-bg-surface text-2xl shadow-lg motion-reduce:animate-none"
      style={current === 0 ? { animation: 'none' } : undefined}
    >
      {face}
    </div>
  );
}

interface CssDiceResult {
  sides: number;
  value: number;
}

interface Props {
  results: CssDiceResult[];
  modifier: number;
  total: number;
  label: string;
}

export function CssDice({ results, modifier, total, label }: Props) {
  return (
    <div className="flex flex-col items-center gap-4">
      <p className="text-sm text-text-secondary">{label}</p>
      <div className="flex items-center gap-3">
        {results.map((r, i) => (
          <SingleDie key={i} sides={r.sides} target={r.value} delay={i * 200} />
        ))}
        {modifier !== 0 && (
          <span className="text-lg text-accent">{modifier > 0 ? `+${modifier}` : modifier}</span>
        )}
      </div>
      <p className="text-2xl font-heading text-text-primary">= {total}</p>
    </div>
  );
}
