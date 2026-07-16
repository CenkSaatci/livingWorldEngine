import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Heart, Shield, Zap, Package, Swords, Sparkles, BookOpen } from 'lucide-react';
import { apiClient } from '../api/client';
import { useToast } from '../hooks/useToast';

interface CharData {
  id: string;
  name: string;
  entity_type: string;
  world_id: string;
  attributes_json: string;
  inventory_json: string;
  faction_id: string;
  backstory: string;
  age: number;
  experience_level: string;
  experience_points: number;
  unspent_attribute_points: number;
}

interface AttributeDef {
  name: string;
  type: string;
  min?: number;
  max?: number;
}
interface SkillDef {
  name: string;
  attribute: string;
  bonus?: number;
}

interface AbilityEntry {
  abilityId: string;
  abilityName: string;
  type: string;
  apCost: number;
  description: string;
}
interface InvEntry {
  itemId: string;
  name: string;
  type: string;
  quantity: number;
  equipped: boolean;
  slot: string | null;
}

export default function CharacterSheetPage() {
  const { t } = useTranslation('character');
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const toast = useToast();

  const [loading, setLoading] = useState(true);
  const [char, setChar] = useState<CharData | null>(null);
  const [attrs, setAttrs] = useState<AttributeDef[]>([]);
  const [skills, setSkills] = useState<SkillDef[]>([]);
  const [inv, setInv] = useState<InvEntry[]>([]);
  const [abilities, setAbilities] = useState<AbilityEntry[]>([]);

  useEffect(() => {
    if (!id) return;
    let cancelled = false;
    (async () => {
      try {
        const cRes = await apiClient.get(`/entities/${id}`);
        if (cancelled) return;
        const c = cRes.data as CharData;
        setChar(c);

        const worldRes = await apiClient.get(`/worlds/${c.world_id}`);
        const gsId: string | null = worldRes.data.game_system_id;
        if (gsId) {
          const gsRes = await apiClient.get(`/game-systems/${gsId}`);
          const rules = JSON.parse(gsRes.data.rules_json);
          setAttrs(rules.attributes ?? []);
          setSkills(rules.skills ?? []);
        }

        apiClient
          .get(`/entities/${id}/inventory`)
          .then((r) => setInv((r.data as { items: InvEntry[] }).items))
          .catch(() => {});
        apiClient
          .get(`/entities/${id}/abilities`)
          .then((r) => setAbilities(r.data as AbilityEntry[]))
          .catch(() => {});

        setLoading(false);
      } catch {
        if (!cancelled) {
          toast.error('Failed to load');
          setLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id]); // eslint-disable-line react-hooks/exhaustive-deps

  if (loading)
    return (
      <div className="flex min-h-screen items-center justify-center bg-bg-primary p-6">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent" />
      </div>
    );
  if (!char)
    return (
      <div className="flex min-h-screen items-center justify-center bg-bg-primary">
        <p className="text-text-secondary">Character not found</p>
      </div>
    );

  const parsedAttrs = JSON.parse(char.attributes_json ?? '{}') as Record<string, number>;
  const attrMod = (v: number) => Math.floor((v - 10) / 2);

  return (
    <div className="min-h-screen bg-bg-primary">
      {/* Header */}
      <header className="flex items-center gap-3 border-b border-bg-elevated bg-bg-surface px-6 py-3">
        <button onClick={() => navigate(-1)} className="text-text-secondary hover:text-accent">
          <ArrowLeft size={20} />
        </button>
        <div className="flex-1">
          <div className="flex items-center gap-3">
            <h1 className="text-lg font-heading text-text-primary">{char.name}</h1>
            <span className="rounded bg-accent/10 px-2 py-0.5 text-xs text-accent">
              {char.entity_type}
            </span>
            {char.faction_id && (
              <span className="rounded bg-bg-elevated px-2 py-0.5 text-xs text-text-secondary">
                #{char.faction_id.slice(0, 8)}
              </span>
            )}
          </div>
        </div>
        <button
          onClick={() => navigate(`/characters/${char.id}/inventory`)}
          className="flex items-center gap-1 rounded border border-bg-elevated px-3 py-1.5 text-xs text-text-secondary hover:text-accent"
        >
          <Package size={14} /> {t('sheet.inventory')}
        </button>
      </header>

      {/* XP Bar */}
      <div className="border-b border-bg-elevated bg-bg-surface/50 px-6 py-2">
        <div className="mx-auto max-w-4xl flex items-center gap-4 text-xs text-text-secondary">
          <Sparkles size={14} className="text-warning" />
          <span className="font-medium text-text-primary">Level 4</span>
          <div className="flex-1 h-2 rounded-full bg-bg-elevated overflow-hidden">
            <div
              className="h-full rounded-full bg-warning transition-all"
              style={{
                width: `${Math.min(100, (((char.experience_points ?? 0) % 100) / 100) * 100)}%`,
              }}
            />
          </div>
          <span>{char.experience_points ?? 0} XP</span>
          {char.unspent_attribute_points > 0 && (
            <span className="text-success font-medium">
              ⚡ {char.unspent_attribute_points} unspent points
            </span>
          )}
        </div>
      </div>

      {/* Main: Two Columns */}
      <main className="mx-auto max-w-5xl p-6">
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          {/* Left Column: Attributes + Combat */}
          <div className="space-y-6">
            {/* Attributes */}
            <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
              <h2 className="mb-4 flex items-center gap-2 font-heading text-text-primary">
                <Swords size={16} className="text-accent" /> Attributes
              </h2>
              <div className="space-y-2">
                {attrs.map((a) => {
                  const val = parsedAttrs[a.name] ?? (a.type === 'INT' ? 10 : 0);
                  return (
                    <div
                      key={a.name}
                      className="flex items-center justify-between rounded bg-bg-primary/50 px-3 py-2"
                    >
                      <span className="text-sm capitalize text-text-primary">{a.name}</span>
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-medium text-text-primary">{val}</span>
                        {a.type === 'INT' && (
                          <span className="text-xs text-text-secondary">
                            ({attrMod(val as number) >= 0 ? '+' : ''}
                            {attrMod(val as number)})
                          </span>
                        )}
                      </div>
                    </div>
                  );
                })}
                {attrs.length === 0 && attrs.length > -1 && attrs.length === 0 && (
                  <p className="text-xs text-text-secondary">No attributes defined</p>
                )}
              </div>
            </section>

            {/* Combat Stats */}
            <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
              <h2 className="mb-4 flex items-center gap-2 font-heading text-text-primary">
                <Shield size={16} className="text-danger" /> Combat
              </h2>
              <div className="grid grid-cols-2 gap-3">
                <div className="rounded bg-bg-primary/50 p-3 text-center">
                  <Heart size={18} className="mx-auto mb-1 text-danger" />
                  <div className="text-lg font-bold text-text-primary">30</div>
                  <div className="text-[10px] text-text-secondary">HP</div>
                </div>
                <div className="rounded bg-bg-primary/50 p-3 text-center">
                  <Zap size={18} className="mx-auto mb-1 text-warning" />
                  <div className="text-lg font-bold text-text-primary">2</div>
                  <div className="text-[10px] text-text-secondary">AP</div>
                </div>
                <div className="rounded bg-bg-primary/50 p-3 text-center">
                  <Shield size={18} className="mx-auto mb-1 text-accent" />
                  <div className="text-lg font-bold text-text-primary">
                    {10 + (parsedAttrs['armor_class'] ?? 0)}
                  </div>
                  <div className="text-[10px] text-text-secondary">AC</div>
                </div>
                <div className="rounded bg-bg-primary/50 p-3 text-center">
                  <Swords size={18} className="mx-auto mb-1 text-accent" />
                  <div className="text-lg font-bold text-text-primary">
                    {attrMod(parsedAttrs['geschicklichkeit'] ?? 10)}
                  </div>
                  <div className="text-[10px] text-text-secondary">Initiative</div>
                </div>
              </div>
            </section>

            {/* Equipped Items */}
            {inv.filter((i) => i.equipped).length > 0 && (
              <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
                <h2 className="mb-3 flex items-center gap-2 font-heading text-text-primary">
                  <Shield size={16} className="text-success" /> Equipped
                </h2>
                <div className="space-y-1">
                  {inv
                    .filter((i) => i.equipped)
                    .map((i) => (
                      <div
                        key={i.itemId}
                        className="flex items-center gap-2 rounded bg-bg-primary/50 px-3 py-1.5 text-sm"
                      >
                        <span className="text-text-primary">{i.name}</span>
                        <span className="text-[10px] text-text-secondary uppercase">
                          ({i.slot})
                        </span>
                      </div>
                    ))}
                </div>
              </section>
            )}
          </div>

          {/* Right Column: Skills + Abilities + Inventory */}
          <div className="space-y-6">
            {/* Skills */}
            <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
              <h2 className="mb-4 flex items-center gap-2 font-heading text-text-primary">
                <BookOpen size={16} className="text-accent" /> Skills
              </h2>
              <div className="space-y-1">
                {skills.map((s) => {
                  const attrVal = parsedAttrs[s.attribute] ?? 10;
                  const total = attrMod(attrVal) + (s.bonus ?? 0);
                  return (
                    <div
                      key={s.name}
                      className="flex items-center justify-between rounded bg-bg-primary/50 px-3 py-1.5"
                    >
                      <div className="flex items-center gap-2">
                        <span className="text-sm text-text-primary">{s.name}</span>
                        <span className="text-[10px] text-text-secondary">({s.attribute})</span>
                      </div>
                      <span
                        className={`text-sm font-mono font-medium ${total >= 0 ? 'text-success' : 'text-danger'}`}
                      >
                        {total >= 0 ? '+' : ''}
                        {total}
                      </span>
                    </div>
                  );
                })}
                {skills.length === 0 && (
                  <p className="text-xs text-text-secondary">No skills defined</p>
                )}
              </div>
            </section>

            {/* Abilities */}
            <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
              <h2 className="mb-4 flex items-center gap-2 font-heading text-text-primary">
                <Zap size={16} className="text-warning" /> Abilities
              </h2>
              {abilities.length === 0 && (
                <p className="text-xs text-text-secondary">No abilities assigned</p>
              )}
              <div className="space-y-2">
                {abilities
                  .filter((a) => a.type === 'ACTIVE')
                  .map((a) => (
                    <div key={a.abilityId} className="rounded bg-bg-primary/50 px-3 py-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-medium text-text-primary">
                          {a.abilityName}
                        </span>
                        <span className="text-[10px] text-accent">AP {a.apCost}</span>
                      </div>
                      {a.description && (
                        <p className="mt-0.5 text-[10px] text-text-secondary">{a.description}</p>
                      )}
                    </div>
                  ))}
                {abilities.filter((a) => a.type === 'PASSIVE').length > 0 && (
                  <>
                    <p className="pt-2 text-[10px] uppercase tracking-wide text-text-secondary">
                      Passive
                    </p>
                    {abilities
                      .filter((a) => a.type === 'PASSIVE')
                      .map((a) => (
                        <div key={a.abilityId} className="rounded bg-bg-primary/50 px-3 py-2">
                          <span className="text-sm text-text-primary">{a.abilityName}</span>
                        </div>
                      ))}
                  </>
                )}
              </div>
            </section>

            {/* Inventory */}
            <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
              <h2 className="mb-3 flex items-center gap-2 font-heading text-text-primary">
                <Package size={16} className="text-accent" /> Inventory ({inv.length})
              </h2>
              <div className="space-y-1">
                {inv
                  .filter((i) => !i.equipped)
                  .map((i) => (
                    <div
                      key={i.itemId}
                      className="flex items-center justify-between rounded bg-bg-primary/50 px-3 py-1.5 text-sm"
                    >
                      <span className="text-text-primary">
                        {i.name} <span className="text-text-secondary">×{i.quantity}</span>
                      </span>
                      <span className="text-[10px] text-text-secondary">{i.type}</span>
                    </div>
                  ))}
                {inv.length === 0 && <p className="text-xs text-text-secondary">Empty</p>}
              </div>
            </section>
          </div>
        </div>

        {/* Backstory */}
        {char.backstory && (
          <section className="mt-6 rounded-lg border border-bg-elevated bg-bg-surface p-5">
            <h2 className="mb-2 font-heading text-text-primary">Backstory</h2>
            <p className="text-sm text-text-secondary leading-relaxed">{char.backstory}</p>
          </section>
        )}
      </main>
    </div>
  );
}
