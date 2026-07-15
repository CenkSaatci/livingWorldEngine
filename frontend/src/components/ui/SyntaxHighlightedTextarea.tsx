import { useRef, useCallback, type TextareaHTMLAttributes } from 'react';

interface Props extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  highlight?: (text: string) => string;
}

function jsonHighlight(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/("(?:[^"\\]|\\.)*")\s*:/g, '<span class="text-accent">$1</span>:')
    .replace(/"(?:[^"\\]|\\.)*"/g, '<span class="text-success">$&</span>')
    .replace(/\b(-?\d+\.?\d*)\b/g, '<span class="text-warning">$1</span>')
    .replace(/\b(true|false)\b/g, '<span class="text-danger">$1</span>')
    .replace(/\b(null)\b/g, '<span class="text-text-secondary">$1</span>');
}

export function SyntaxHighlightedTextarea(props: Props) {
  const { highlight = jsonHighlight, className = '', ...textareaProps } = props;
  const highlighterRef = useRef<HTMLDivElement | null>(null);
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);

  const syncScroll = useCallback(() => {
    if (highlighterRef.current && textareaRef.current) {
      highlighterRef.current.scrollTop = textareaRef.current.scrollTop;
      highlighterRef.current.scrollLeft = textareaRef.current.scrollLeft;
    }
  }, []);

  return (
    <div className="relative">
      <div
        ref={highlighterRef}
        className={`pointer-events-none absolute inset-0 overflow-auto whitespace-pre-wrap break-words border border-transparent px-3 py-2 text-xs font-mono ${className.replace(/border-[\w-]+/g, '')}`}
        style={{ zIndex: 1 }}
        dangerouslySetInnerHTML={{
          __html: highlight((textareaProps.value as string) || '') + '\n',
        }}
      />
      <textarea
        ref={textareaRef}
        onScroll={syncScroll}
        className={`relative bg-transparent ${className}`}
        style={{ zIndex: 2, color: 'transparent', caretColor: 'white' }}
        {...textareaProps}
      />
    </div>
  );
}
