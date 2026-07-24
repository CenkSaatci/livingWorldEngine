import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Download } from 'lucide-react';
import { CharacterSheet } from '../components/character/CharacterSheet';
import { apiClient } from '../api/client';
import { useToast } from '../hooks/useToast';

export default function CharacterSheetPage() {
  const { t } = useTranslation('character');
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const toast = useToast();

  const handleExport = async () => {
    if (!id) return;
    try {
      const res = await apiClient.get(`/entities/${id}`);
      const blob = new Blob([JSON.stringify(res.data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = `character-${id.slice(0, 8)}.json`; a.click();
      URL.revokeObjectURL(url);
    } catch { toast.error('Export failed'); }
  };

  return (
    <div className="min-h-screen bg-bg-primary">
      <header className="flex items-center gap-3 border-b border-bg-elevated bg-bg-surface px-6 py-3">
        <button onClick={() => navigate(-1)} className="text-text-secondary hover:text-accent">
          <ArrowLeft size={20} />
        </button>
        <h1 className="flex-1 text-lg font-heading text-text-primary">{t('sheet.title')}</h1>
        <button onClick={handleExport} className="flex items-center gap-1 rounded border border-bg-elevated px-3 py-1.5 text-xs text-text-secondary hover:text-accent">
          <Download size={14} /> {t('sheet.export')}
        </button>
      </header>
      <CharacterSheet entityId={id ?? ''} worldId="" />
    </div>
  );
}
