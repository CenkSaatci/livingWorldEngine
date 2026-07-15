import { Coins, TrendingUp, TrendingDown } from 'lucide-react';

interface Props {
  basePrice: number;
  finalPrice: number;
}

export function PriceTag({ basePrice, finalPrice }: Props) {
  const diff = finalPrice - basePrice;
  const isPremium = diff > 0;

  return (
    <span className="inline-flex items-center gap-1 text-sm">
      {isPremium ? (
        <TrendingUp size={14} className="text-danger" />
      ) : diff < 0 ? (
        <TrendingDown size={14} className="text-success" />
      ) : (
        <Coins size={14} className="text-yellow-500" />
      )}
      <span className={isPremium ? 'text-danger' : 'text-text-primary'}>{finalPrice} G</span>
      {diff !== 0 && (
        <span className="text-[10px] text-text-secondary">
          ({isPremium ? '+' : ''}
          {diff})
        </span>
      )}
    </span>
  );
}
