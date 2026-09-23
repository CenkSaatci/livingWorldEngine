import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, ShoppingCart } from 'lucide-react';
import { MerchantPanel } from '../components/world/MerchantPanel';

export default function MarketPage() {
  const { t } = useTranslation('common');
  const { id, locationId } = useParams<{ id: string; locationId: string }>();
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-bg-primary">
      <header className="flex items-center gap-3 border-b border-bg-elevated bg-bg-surface px-6 py-3">
        <button onClick={() => navigate(-1)} className="text-text-secondary hover:text-accent" aria-label={t('actions.back')} title={t('actions.back')}>
          <ArrowLeft size={20} />
        </button>
        <h1 className="flex items-center gap-2 text-lg font-heading text-text-primary">
          <ShoppingCart size={20} className="text-accent" /> {t('market.title')}
        </h1>
      </header>

      <main className="mx-auto max-w-3xl p-6">
        <MerchantPanel worldId={id ?? ''} locationId={locationId ?? ''} />
      </main>
    </div>
  );
}
