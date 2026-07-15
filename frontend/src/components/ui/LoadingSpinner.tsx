const sizeMap = {
  sm: 'h-4 w-4 border-2',
  md: 'h-8 w-8 border-3',
  lg: 'h-12 w-12 border-4',
};

interface Props {
  size?: keyof typeof sizeMap;
  text?: string;
}

export function LoadingSpinner({ size = 'md', text }: Props) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-8">
      <div
        className={`animate-spin rounded-full border-accent border-t-transparent ${sizeMap[size]}`}
      />
      {text && <span className="text-sm text-text-secondary">{text}</span>}
    </div>
  );
}
