import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Package, Save } from 'lucide-react';
import { apiClient } from '../api/client';
import { useApiGet } from '../hooks/useApiGet';
import { useToast } from '../hooks/useToast';
import { AttributeField } from '../components/character/AttributeField';
import { SkillList } from '../components/character/SkillList';

interface CharData {
  id: string;
  name: string;
  entity_type: string;
  attributes_json: string;
  inventory_json: string;
  world_id: string;
}

interface AttributeDef {
  name: string;
  type: 'INT' | 'STRING' | 'BOOL';
  min?: number;
  max?: number;
}

interface SkillDef {
  name: string;
  attribute: string;
  bonus?: number;
}

export default function CharacterSheetPage() {
  const { t } = useTranslation('character');
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const toast = useToast();
  const { data: char, loading } = useApiGet<CharData>(id ? `/entities/${id}` : '', []);
  const [attrs, setAttrs] = useState<AttributeDef[]>([]);
  const [skills, setSkills] = useState<SkillDef[]>([]);
  const [values, setValues] = useState<Record<string, number | string | boolean>>({});
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!char) return;
    (async () => {
      try {
        const parsed = JSON.parse(char.attributes_json ?? '{}');
        setValues(parsed);

        const worldRes = await apiClient.get(`/worlds/${char.world_id}`);
        const gsId: string | null = worldRes.data.game_system_id;
        if (gsId) {
          const gsRes = await apiClient.get(`/game-systems/${gsId}`);
          const rules = JSON.parse(gsRes.data.rules_json);
          setAttrs(rules.attributes ?? []);
          setSkills(rules.skills ?? []);
        }
      } catch {
        toast.error('Failed to load character data');
      }
    })();
  }, [char]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSave = async () => {
    if (!id || !char) return;
    setSaving(true);
    try {
      await apiClient.patch(`/worlds/${char.world_id}/entities/${id}`, {
        attributesJson: JSON.stringify(values),
      });
      toast.success(t('sheet.saved'));
    } catch {
      toast.error(t('sheet.save_error'));
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-bg-primary">
        <p className="text-text-secondary">{t('sheet.title')}…</p>
      </div>
    );
  }

  if (!char) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-bg-primary">
        <p className="text-text-secondary">Character not found</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-bg-primary">
      <header className="flex items-center gap-3 border-b border-bg-elevated bg-bg-surface px-6 py-3">
        <button onClick={() => navigate(-1)} className="text-text-secondary hover:text-accent">
          <ArrowLeft size={20} />
        </button>
        <h1 className="text-lg font-heading text-text-primary">{char.name}</h1>
        <span className="rounded bg-bg-elevated px-2 py-0.5 text-xs text-text-secondary">
          {char.entity_type}
        </span>
        <div className="flex-1" />
        <button
          onClick={handleSave}
          disabled={saving}
          className="flex items-center gap-1 rounded bg-accent px-3 py-1.5 text-xs text-white hover:bg-accent/80 disabled:opacity-40"
        >
          <Save size={14} /> {saving ? t('sheet.saving') : t('sheet.save')}
        </button>
      </header>

      <main className="mx-auto max-w-2xl space-y-6 p-6">
        {/* Attributes */}
        <section className="rounded-lg bg-bg-surface p-5">
          <h2 className="mb-3 font-heading text-text-primary">{t('sheet.attributes')}</h2>
          <div className="divide-y divide-bg-elevated">
            {attrs.map((attr) => (
              <AttributeField
                key={attr.name}
                label={attr.name}
                type={attr.type}
                value={(values[attr.name] ?? attr.type === 'INT') ? 10 : ''}
                onChange={(v: number | string | boolean) =>
                  setValues((prev) => ({ ...prev, [attr.name]: v }))
                }
              />
            ))}
          </div>
        </section>

        {/* Skills */}
        {skills.length > 0 && (
          <section className="rounded-lg bg-bg-surface p-5">
            <h2 className="mb-3 font-heading text-text-primary">{t('sheet.skills')}</h2>
            <SkillList
              skills={skills}
              entityId={char.id}
              worldId={char.world_id}
              attributes={
                Object.fromEntries(
                  Object.entries(values).filter(([, v]) => typeof v === 'number'),
                ) as Record<string, number>
              }
            />
          </section>
        )}

        {/* Inventory Link */}
        <section className="rounded-lg bg-bg-surface p-5">
          <button
            onClick={() => navigate(`/characters/${char.id}/inventory`)}
            className="flex w-full items-center justify-between text-text-primary hover:text-accent"
          >
            <span className="flex items-center gap-2 font-heading">
              <Package size={18} /> {t('sheet.inventory')}
            </span>
            <span className="text-text-secondary">→</span>
          </button>
        </section>
      </main>
    </div>
  );
}
