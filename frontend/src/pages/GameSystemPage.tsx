import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Plus,
  Check,
  X,
  Loader,
  FileText,
  Code,
  Trash2,
  Pencil,
  Download,
  Upload,
  Copy,
} from 'lucide-react';
import { SyntaxHighlightedTextarea } from '../components/ui/SyntaxHighlightedTextarea';
import { apiClient } from '../api/client';
import { useToast } from '../hooks/useToast';
import { SystemWizard, type SystemWizardHandle, type WizardData } from '../components/game/SystemWizard';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';

interface GameSystem {
  id: string;
  name: string;
  version: number;
  active?: boolean;
}

interface GameSystemDetail extends GameSystem {
  rulesJson: string;
  active: boolean;
}

const TEMPLATES: Record<string, string> = {
  D20Lite: JSON.stringify(
    {
      version: 1,
      attributes: [
        { name: 'staerke', type: 'INT', min: 1, max: 20, default: 10 },
        { name: 'geschicklichkeit', type: 'INT', min: 1, max: 20, default: 10 },
        { name: 'konstitution', type: 'INT', min: 1, max: 20, default: 10 },
        { name: 'intelligenz', type: 'INT', min: 1, max: 20, default: 10 },
        { name: 'weisheit', type: 'INT', min: 1, max: 20, default: 10 },
        { name: 'charisma', type: 'INT', min: 1, max: 20, default: 10 },
      ],
      skills: [
        { name: 'athletik', attribute: 'staerke' },
        { name: 'schleichen', attribute: 'geschicklichkeit' },
        { name: 'wahrnehmung', attribute: 'weisheit' },
        { name: 'ueberzeugen', attribute: 'charisma' },
      ],
      dice_mechanics: {
        probe: '1d20+mod',
        combat: {
          initiative: '1d20+geschicklichkeit',
          damage: '1d8+staerke',
          action_points: { standard: 1, max: 2 },
        },
      },
    },
    null,
    2,
  ),

  TwoDicePool: JSON.stringify(
    {
      version: 1,
      attributes: [
        { name: 'staerke', type: 'INT', min: 1, max: 12, default: 6 },
        { name: 'geschick', type: 'INT', min: 1, max: 12, default: 6 },
        { name: 'verstand', type: 'INT', min: 1, max: 12, default: 6 },
      ],
      skills: [
        { name: 'kaempfen', attribute: 'staerke' },
        { name: 'schleichen', attribute: 'geschick' },
        { name: 'wissen', attribute: 'verstand' },
      ],
      dice_mechanics: {
        probe: '2d6+mod',
        combat: {
          initiative: '2d6+geschick',
          damage: '1d6+staerke',
          action_points: { standard: 2, max: 4 },
        },
      },
    },
    null,
    2,
  ),

  Fudge: JSON.stringify(
    {
      version: 1,
      attributes: [
        { name: 'geschick', type: 'INT', min: 1, max: 8, default: 3 },
        { name: 'schnelligkeit', type: 'INT', min: 1, max: 8, default: 3 },
        { name: 'verstand', type: 'INT', min: 1, max: 8, default: 3 },
        { name: 'wille', type: 'INT', min: 1, max: 8, default: 3 },
      ],
      skills: [
        { name: 'athletik', attribute: 'geschick' },
        { name: 'kampf', attribute: 'schnelligkeit' },
        { name: 'wissen', attribute: 'verstand' },
        { name: 'mut', attribute: 'wille' },
      ],
      dice_mechanics: {
        probe: '4dF',
        combat: {
          initiative: '4dF+schnelligkeit',
          damage: '1d6+geschick',
        },
      },
    },
    null,
    2,
  ),
};

export default function GameSystemPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const [systems, setSystems] = useState<GameSystem[]>([]);
  const [loading, setLoading] = useState(true);
  const [showEditor, setShowEditor] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [name, setName] = useState('');
  const [version, setVersion] = useState(1);
  const [rulesJson, setRulesJson] = useState('');
  const [template, setTemplate] = useState('D20Lite');
  const [validation, setValidation] = useState<{ valid: boolean; errors?: string[] } | null>(null);
  const [validating, setValidating] = useState(false);
  const [deleting, setDeleting] = useState<string | null>(null);
  const [editorMode, setEditorMode] = useState<'wizard' | 'json'>('wizard');
  const [wizardData, setWizardData] = useState<WizardData | null>(null);
  const wizardRef = useRef<SystemWizardHandle | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const parseRulesToWizard = (json: string): WizardData | null => {
    try {
      const parsed = JSON.parse(json);
      const attrs: WizardData['attributes'] = (parsed.attributes ?? []).map(
        (a: Record<string, unknown>) => ({
          name: (a.name as string) ?? '',
          type: (['INT', 'STRING', 'BOOL'].includes(a.type as string) ? a.type : 'INT') as 'INT' | 'STRING' | 'BOOL',
          min: (a.min as number) ?? 1,
          max: (a.max as number) ?? 20,
          default: (a.default as number) ?? 10,
        })
      );
      const skills: WizardData['skills'] = (parsed.skills ?? []).map(
        (s: Record<string, unknown>) => ({
          name: (s.name as string) ?? '',
          attributes: (s.attributes as string[]) ?? ((s.attribute as string) ? [s.attribute as string] : []),
          bonus: (s.bonus as number) ?? 0,
        })
      );
      const dice = parsed.dice_mechanics ?? {};
      const combat = dice.combat ?? {};
      return {
        name: parsed.name ?? '',
        version: parsed.version ?? 1,
        description: parsed.description ?? '',
        attributes: attrs,
        skills,
        probe: dice.probe ?? '1d20+mod',
        enableCombat: !!dice.combat,
        combat: {
          initiative: combat.initiative ?? '1d20+geschick',
          damage: combat.damage ?? '1d8+staerke',
          actionPoints: {
            standard: combat.action_points?.standard ?? 1,
            max: combat.action_points?.max ?? 2,
          },
        },
        progressionType: parsed.progressionType ?? null,
        features: parsed.features ?? { magic: false, psionics: false, rangedCombat: false, criticalHits: false, armorPenalty: false },
        derivedValues: (parsed.derived_values ?? []).map((dv: Record<string, unknown>) => ({
          name: (dv.name as string) ?? '',
          formula: (dv.formula as string) ?? '',
        })),
        abilities: (parsed.abilities ?? []).map((a: Record<string, unknown>) => ({
          name: (a.name as string) ?? '',
          type: (a.type as 'active' | 'passive') ?? 'active',
          costType: (a.costType as 'AP' | 'MP' | '') ?? 'AP',
          cost: (a.cost as number) ?? 0,
          diceExpression: (a.diceExpression as string) ?? '',
          effect: (a.effect as string) ?? '',
          bonus: (a.bonus as string) ?? '',
        })),
      };
    } catch { return null; }
  };

  const switchToJson = () => {
    if (wizardRef.current) {
      setRulesJson(wizardRef.current.buildRulesJson());
    }
    setEditorMode('json');
  };

  const handleClone = async (id: string) => {
    try {
      await apiClient.post(`/game-systems/${id}/clone`);
      toast.success('System duplicated');
      fetchSystems();
    } catch {
      toast.error('Failed to duplicate');
    }
  };

  const handleExport = async (sys: GameSystem) => {
    try {
      const res = await apiClient.get<GameSystemDetail>(`/game-systems/${sys.id}`);
      const blob = new Blob([JSON.stringify(res.data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${sys.name}.json`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      toast.error('Failed to export');
    }
  };

  const handleImport = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const text = await file.text();
      const data = JSON.parse(text);
      await apiClient.post('/game-systems', {
        name: data.name ?? 'Imported System',
        version: data.version ?? 1,
        rulesJson: data.rulesJson ?? data.rules_json ?? '{}',
        schemaJson: '{}',
      });
      toast.success('System imported');
      fetchSystems();
    } catch {
      toast.error('Failed to import');
    }
    e.target.value = '';
  };

  const fetchSystems = useCallback(async () => {
    try {
      const res = await apiClient.get('/game-systems');
      setSystems(res.data);
    } catch {
      /* */
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSystems();
  }, [fetchSystems]);

  const loadTemplate = () => {
    setRulesJson(TEMPLATES[template] ?? '');
    setValidation(null);
  };

  const handleValidate = async () => {
    setValidating(true);
    setValidation(null);
    try {
      const res = await apiClient.post('/game-systems/validate', {
        rulesJson,
        schemaJson: '{}',
      });
      setValidation(res.data);
    } catch (e: unknown) {
      const msg =
        (e as { response?: { data?: { error?: string } } })?.response?.data?.error ??
        'Invalid JSON';
      setValidation({ valid: false, errors: [msg] });
    } finally {
      setValidating(false);
    }
  };

  const handleSave = async () => {
    if (!name.trim() || !rulesJson.trim()) return;
    try {
      if (editingId) {
        await apiClient.patch(`/game-systems/${editingId}`, {
          name: name.trim(),
          version,
          rulesJson,
        });
        toast.success('Game system updated');
      } else {
        await apiClient.post('/game-systems', {
          name: name.trim(),
          version,
          rulesJson,
          schemaJson: '{}',
        });
        toast.success('Game system created');
      }
      setShowEditor(false);
      setEditingId(null);
      setName('');
      setRulesJson('');
      setValidation(null);
      fetchSystems();
    } catch {
      toast.error(editingId ? 'Failed to update game system' : 'Failed to create game system');
    }
  };

  const handleEdit = async (sys: GameSystem) => {
    try {
      const res = await apiClient.get<GameSystemDetail>(`/game-systems/${sys.id}`);
      const detail = res.data;
      setEditingId(detail.id);
      setName(detail.name);
      setVersion(detail.version);
      setRulesJson(detail.rulesJson ?? '');
      const parsed = parseRulesToWizard(detail.rulesJson);
      if (parsed) {
        parsed.name = detail.name;
        parsed.version = detail.version;
      }
      setWizardData(parsed);
      setValidation(null);
      setEditorMode('wizard');
      setShowEditor(true);
    } catch {
      toast.error('Failed to load game system details');
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await apiClient.delete(`/game-systems/${id}`);
      toast.success('Game system deleted');
      setDeleting(null);
      fetchSystems();
    } catch {
      toast.error('Failed to delete game system');
    }
  };

  const closeEditor = () => {
    setShowEditor(false);
    setEditingId(null);
    setName('');
    setRulesJson('');
    setWizardData(null);
    setValidation(null);
  };

  return (
    <div className="min-h-screen bg-bg-primary">
      <header className="flex items-center justify-between border-b border-bg-elevated bg-bg-surface px-6 py-3">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/dashboard')}
            className="text-text-secondary hover:text-accent"
          >
            <ArrowLeft size={20} />
          </button>
          <h1 className="flex items-center gap-2 text-lg font-heading text-text-primary">
            <FileText size={20} /> Game Systems
          </h1>
        </div>
        <button
          onClick={() => {
            setShowEditor(true);
            setEditorMode('wizard');
            setEditingId(null);
            setName('');
            setVersion(1);
            setWizardData(null);
          }}
          className="flex items-center gap-1 rounded bg-accent px-3 py-1.5 text-xs text-white hover:bg-accent/80"
        >
          <Plus size={14} /> New System
        </button>
        <button
          onClick={() => fileInputRef.current?.click()}
          className="flex items-center gap-1 rounded border border-bg-elevated px-3 py-1.5 text-xs text-text-secondary hover:text-text-primary"
        >
          <Upload size={14} /> Import
        </button>
        <input
          ref={fileInputRef}
          type="file"
          accept=".json"
          onChange={handleImport}
          className="hidden"
        />
      </header>

      <main className="mx-auto max-w-4xl space-y-6 p-6">
        {/* System List */}
        {loading ? (
          <LoadingSpinner />
        ) : (
          <div className="grid gap-3 sm:grid-cols-2">
            {systems.map((sys) => (
              <div key={sys.id} className="rounded-lg border border-bg-elevated bg-bg-surface p-4">
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="font-heading text-text-primary">{sys.name}</h3>
                    <p className="text-xs text-text-secondary">v{sys.version}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => handleExport(sys)}
                      className="text-text-secondary hover:text-accent"
                      title="Export"
                    >
                      <Download size={14} />
                    </button>
                    <button
                      onClick={() => handleClone(sys.id)}
                      className="text-text-secondary hover:text-accent"
                      title="Duplicate"
                    >
                      <Copy size={14} />
                    </button>
                    <button
                      onClick={() => handleEdit(sys)}
                      className="text-text-secondary hover:text-accent"
                    >
                      <Pencil size={14} />
                    </button>
                    <button
                      onClick={() => setDeleting(sys.id)}
                      className="text-text-secondary hover:text-danger"
                    >
                      <Trash2 size={14} />
                    </button>
                    <span
                      className={`h-2 w-2 rounded-full ${sys.active ? 'bg-success' : 'bg-text-secondary'}`}
                    />
                  </div>
                </div>
              </div>
            ))}
            {systems.length === 0 && (
              <p className="col-span-2 text-center text-sm text-text-secondary py-8">
                No game systems yet
              </p>
            )}
          </div>
        )}

        {/* Editor */}
        {showEditor && (
          <div className="rounded-lg border border-accent/20 bg-bg-surface p-5">
            <div className="flex items-center justify-between mb-4">
              <h2 className="font-heading text-text-primary">
                {editingId ? `Edit: ${name}` : 'New Game System'}
              </h2>
              {!editingId && (
                <div className="flex gap-1 rounded bg-bg-elevated p-0.5">
                  <button
                    onClick={() => setEditorMode('wizard')}
                    className={`rounded px-3 py-1 text-xs ${editorMode === 'wizard' ? 'bg-accent text-white' : 'text-text-secondary hover:text-text-primary'}`}
                  >
                    Wizard
                  </button>
                  <button
                    onClick={switchToJson}
                    className={`rounded px-3 py-1 text-xs ${editorMode === 'json' ? 'bg-accent text-white' : 'text-text-secondary hover:text-text-primary'}`}
                  >
                    <Code size={12} className="inline mr-1" />
                    JSON
                  </button>
                </div>
              )}
            </div>

            {editorMode === 'wizard' ? (
              <SystemWizard
                ref={wizardRef}
                initialData={wizardData as WizardData | undefined}
                systemId={editingId ?? undefined}
                onSaved={() => {
                  setShowEditor(false);
                  setEditingId(null);
                  setWizardData(null);
                  fetchSystems();
                }}
                onClose={() => { setShowEditor(false); setEditingId(null); setWizardData(null); }}
              />
            ) : (
              <>
                <div className="grid grid-cols-2 gap-4 mb-4">
                  <div>
                    <label className="block text-xs text-text-secondary mb-1">Name</label>
                    <input
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-text-secondary mb-1">Version</label>
                    <input
                      type="number"
                      min={1}
                      value={version}
                      onChange={(e) => setVersion(Number(e.target.value))}
                      className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                    />
                  </div>
                </div>

                {/* Template Loader */}
                {!editingId && (
                  <div className="flex items-center gap-2 mb-4">
                    <select
                      value={template}
                      onChange={(e) => setTemplate(e.target.value)}
                      className="rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                    >
                      {Object.keys(TEMPLATES).map((t) => (
                        <option key={t} value={t}>
                          {t}
                        </option>
                      ))}
                    </select>
                    <button
                      onClick={loadTemplate}
                      className="rounded bg-bg-elevated px-3 py-2 text-xs text-text-secondary hover:text-text-primary"
                    >
                      Load Template
                    </button>
                  </div>
                )}

                {/* JSON Editor */}
                <div className="mb-3">
                  <label className="block text-xs text-text-secondary mb-1">Rules JSON</label>
                  <SyntaxHighlightedTextarea
                    value={rulesJson}
                    onChange={(e) => {
                      setRulesJson(e.target.value);
                      setValidation(null);
                    }}
                    rows={18}
                    className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-xs font-mono text-text-primary outline-none focus:border-accent resize-none"
                    placeholder='{ "version": 1, "attributes": [...], "dice_mechanics": {...} }'
                  />
                </div>

                {/* Validation + Save */}
                <div className="flex items-center gap-3">
                  <button
                    onClick={handleValidate}
                    disabled={validating}
                    className="flex items-center gap-1 rounded border border-bg-elevated px-3 py-2 text-xs text-text-secondary hover:text-text-primary"
                  >
                    {validating ? (
                      <Loader size={14} className="animate-spin" />
                    ) : (
                      <Copy size={14} />
                    )}
                    Validate
                  </button>
                  <button
                    onClick={() => {
                      try {
                        setRulesJson(JSON.stringify(JSON.parse(rulesJson), null, 2));
                      } catch {
                        /* ignore */
                      }
                    }}
                    className="flex items-center gap-1 rounded border border-bg-elevated px-3 py-2 text-xs text-text-secondary hover:text-text-primary"
                  >
                    Format JSON
                  </button>

                  {validation && (
                    <span
                      className={`flex items-center gap-1 text-xs ${validation.valid ? 'text-success' : 'text-danger'}`}
                    >
                      {validation.valid ? <Check size={14} /> : <X size={14} />}
                      {validation.valid ? 'Valid' : (validation.errors?.[0] ?? 'Invalid')}
                    </span>
                  )}

                  <div className="flex-1" />

                  <button
                    onClick={closeEditor}
                    className="rounded border border-bg-elevated px-3 py-2 text-xs text-text-secondary hover:text-text-primary"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={handleSave}
                    disabled={!name?.trim() || !rulesJson?.trim()}
                    className="rounded bg-accent px-4 py-2 text-xs text-white hover:bg-accent/80 disabled:opacity-40"
                  >
                    {editingId ? 'Update System' : 'Save System'}
                  </button>
                </div>
              </>
            )}
          </div>
        )}

        {/* Delete Confirm */}
        {deleting && (
          <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/60"
            onClick={() => setDeleting(null)}
          >
            <div
              className="w-72 rounded-xl border border-bg-elevated bg-bg-surface p-5 shadow-2xl"
              onClick={(e) => e.stopPropagation()}
            >
              <h3 className="font-heading text-text-primary mb-2">Delete Game System?</h3>
              <p className="text-sm text-text-secondary mb-4">
                This will permanently delete this game system.
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => setDeleting(null)}
                  className="flex-1 rounded border border-bg-elevated py-2 text-sm text-text-secondary hover:text-text-primary"
                >
                  Cancel
                </button>
                <button
                  onClick={() => handleDelete(deleting)}
                  className="flex-1 rounded bg-danger py-2 text-sm text-white hover:bg-danger/80"
                >
                  Delete
                </button>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
