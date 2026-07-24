import { useRef } from 'react';

interface AttributeRef {
  name: string;
}

interface Props {
  value: string;
  onChange: (value: string) => void;
  attributes: AttributeRef[];
  preview?: string;
}

const OPERATORS = [
  { label: '+', value: '+' },
  { label: '−', value: '-' },
  { label: '×', value: '*' },
  { label: '÷', value: '/' },
  { label: '(', value: '(' },
  { label: ')', value: ')' },
];

const FUNCTIONS = ['min(', 'max(', 'floor('];

export function FormulaBuilder({ value, onChange, attributes, preview }: Props) {
  const inputRef = useRef<HTMLInputElement>(null);

  const insertAtCursor = (text: string) => {
    const el = inputRef.current;
    if (!el) return;
    const start = el.selectionStart ?? value.length;
    const end = el.selectionEnd ?? value.length;
    const newVal = value.slice(0, start) + text + value.slice(end);
    onChange(newVal);
    requestAnimationFrame(() => {
      el.focus();
      const pos = start + text.length;
      el.setSelectionRange(pos, pos);
    });
  };

  const insertAttr = (name: string) => insertAtCursor(`@{${name}}`);

  return (
    <div className="space-y-2">
      {/* Formula input */}
      <input
        ref={inputRef}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm font-mono text-text-primary outline-none focus:border-accent"
        placeholder="z.B. (@{konstitution}+@{koerperkraft})/2+5"
      />

      {/* Attribute buttons */}
      {attributes.length > 0 && (
        <div className="flex flex-wrap gap-1">
          <span className="text-[10px] text-text-secondary self-center mr-1">Attribute:</span>
          {attributes.map((a) => (
            <button
              key={a.name}
              onClick={() => insertAttr(a.name)}
              className="rounded bg-accent/10 px-2 py-0.5 text-[10px] text-accent hover:bg-accent/20 font-mono"
              title={a.name}
            >
              @{a.name}
            </button>
          ))}
        </div>
      )}

      {/* Operator buttons */}
      <div className="flex flex-wrap gap-1">
        <span className="text-[10px] text-text-secondary self-center mr-1">Operatoren:</span>
        {OPERATORS.map((op) => (
          <button
            key={op.value}
            onClick={() => insertAtCursor(op.value)}
            className="rounded bg-bg-elevated/50 px-2 py-0.5 text-xs text-text-primary hover:bg-bg-elevated font-mono"
          >
            {op.label}
          </button>
        ))}
        {FUNCTIONS.map((fn) => (
          <button
            key={fn}
            onClick={() => insertAtCursor(fn)}
            className="rounded bg-bg-elevated/50 px-2 py-0.5 text-xs text-text-primary hover:bg-bg-elevated font-mono"
          >
            {fn}
          </button>
        ))}
      </div>

      {/* Preview */}
      {preview !== undefined && (
        <p className="text-[10px] text-text-secondary">
          Vorschau: <span className="font-mono text-text-primary">{preview}</span>
        </p>
      )}
    </div>
  );
}
