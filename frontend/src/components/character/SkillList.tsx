import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Dices } from 'lucide-react';
import { apiClient } from '../../api/client';

interface Skill {
  name: string;
  attribute: string;
  bonus?: number;
}

interface Props {
  skills: Skill[];
  entityId: string;
  worldId: string;
  attributes: Record<string, number>;
  disabled?: boolean;
}

export function SkillList({ skills, entityId, worldId, attributes, disabled }: Props) {
  const { t } = useTranslation('character');
  const [rolling, setRolling] = useState<string | null>(null);
  const [results, setResults] = useState<Record<string, string>>({});

  const handleRoll = async (skill: Skill) => {
    setRolling(skill.name);
    try {
      const attrValue = attributes[skill.attribute] ?? 10;
      const res = await apiClient.post('/rolls', {
        worldId,
        entityId,
        skillId: skill.attribute,
        modifier: skill.bonus ?? 0,
        target: attrValue,
      });
      const data = res.data;
      const expr = data.expression ?? '?';
      const total = data.total ?? 0;
      setResults((prev) => ({
        ...prev,
        [skill.name]: `${total} (${expr})`,
      }));
    } catch {
      setResults((prev) => ({ ...prev, [skill.name]: 'ERR' }));
    } finally {
      setRolling(null);
    }
  };

  return (
    <div className="space-y-1">
      {skills.map((skill) => (
        <div
          key={skill.name}
          className="flex items-center justify-between rounded bg-bg-primary/50 px-3 py-2"
        >
          <div>
            <span className="text-sm text-text-primary capitalize">{skill.name}</span>
            <span className="ml-2 text-xs text-text-secondary">
              ({skill.attribute}{skill.bonus ? `+${skill.bonus}` : ''})
            </span>
          </div>

          <div className="flex items-center gap-3">
            {results[skill.name] && (
              <span className="text-xs font-mono text-text-secondary">
                {results[skill.name]}
              </span>
            )}
            <button
              onClick={() => handleRoll(skill)}
              disabled={disabled || rolling === skill.name}
              className="flex items-center gap-1 rounded bg-accent/20 px-2 py-1 text-xs
                         text-accent hover:bg-accent/40 disabled:opacity-40"
              title={t('sheet.roll_button')!}
            >
              <Dices size={14} />
              {rolling === skill.name ? '…' : t('sheet.roll_button')}
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}
