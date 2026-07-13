interface WidgetProps {
  label: string;
  type: 'INT' | 'STRING' | 'BOOL';
  value: number | string | boolean;
  onChange: (value: number | string | boolean) => void;
}

export function AttributeField({ label, type, value, onChange }: WidgetProps) {
  if (type === 'INT') {
    const numVal = typeof value === 'number' ? value : 10;
    return (
      <div className="flex items-center justify-between py-1">
        <span className="text-sm text-text-primary capitalize">{label}</span>
        <div className="flex items-center gap-2">
          <button
            onClick={() => onChange(Math.max(1, numVal - 1))}
            className="flex h-6 w-6 items-center justify-center rounded bg-bg-elevated text-xs text-text-secondary hover:text-text-primary"
          >
            −
          </button>
          <span className="w-8 text-center text-sm font-mono text-text-primary">{numVal}</span>
          <button
            onClick={() => onChange(Math.min(30, numVal + 1))}
            className="flex h-6 w-6 items-center justify-center rounded bg-bg-elevated text-xs text-text-secondary hover:text-text-primary"
          >
            +
          </button>
        </div>
      </div>
    );
  }

  if (type === 'BOOL') {
    return (
      <div className="flex items-center justify-between py-1">
        <span className="text-sm text-text-primary capitalize">{label}</span>
        <button
          onClick={() => onChange(!value)}
          className={`h-5 w-9 rounded-full transition-colors ${
            value ? 'bg-accent' : 'bg-bg-elevated'
          }`}
        >
          <div
            className={`h-4 w-4 translate-y-0.5 rounded-full bg-white transition-transform ${
              value ? 'translate-x-4' : 'translate-x-0.5'
            }`}
          />
        </button>
      </div>
    );
  }

  // STRING
  return (
    <div className="py-1">
      <label className="mb-1 block text-sm text-text-primary capitalize">{label}</label>
      <input
        type="text"
        value={String(value ?? '')}
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-sm text-text-primary"
      />
    </div>
  );
}
