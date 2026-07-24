import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft } from 'lucide-react';
import { CharacterSheet } from '../components/character/CharacterSheet';

export default function CharacterSheetPage() {
  const { t } = useTranslation('character');
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-bg-primary">
      <header className="flex items-center gap-3 border-b border-bg-elevated bg-bg-surface px-6 py-3">
        <button onClick={() => navigate(-1)} className="text-text-secondary hover:text-accent">
          <ArrowLeft size={20} />
        </button>
        <h1 className="text-lg font-heading text-text-primary">{t('sheet.title')}</h1>
      </header>
      <CharacterSheet entityId={id ?? ''} worldId="" />
    </div>
  );
}
