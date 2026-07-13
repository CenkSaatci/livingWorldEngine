import { useTranslation } from 'react-i18next';
import { Eye, EyeOff } from 'lucide-react';

interface Props {
  fogActive: boolean;
  onToggle: () => void;
}

export function FogControls({ fogActive, onToggle }: Props) {
  const { t } = useTranslation('map');

  return (
    <div className="flex items-center gap-2 rounded bg-bg-surface px-3 py-2 shadow-lg">
      <button
        onClick={onToggle}
        className="flex items-center gap-1.5 text-sm text-text-secondary hover:text-accent"
        title={fogActive ? t('fog.hide')! : t('fog.show')!}
      >
        {fogActive ? <EyeOff size={18} /> : <Eye size={18} />}
        {fogActive ? t('fog.hide') : t('fog.show')}
      </button>
    </div>
  );
}
