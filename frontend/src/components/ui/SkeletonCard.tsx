interface Props {
  lines?: number;
}

export function SkeletonCard({ lines = 3 }: Props) {
  return (
    <div className="animate-pulse rounded-lg border border-bg-elevated bg-bg-surface p-4">
      <div className="h-4 w-3/4 rounded bg-bg-elevated mb-3" />
      {Array.from({ length: lines }).map((_, i) => (
        <div
          key={i}
          className="h-3 rounded bg-bg-elevated/60 mb-2"
          style={{ width: `${70 - i * 15}%` }}
        />
      ))}
    </div>
  );
}
