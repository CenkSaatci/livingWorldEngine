import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { CharacterWizard } from './CharacterWizard';
import { apiClient } from '../../api/client';
import { defaultWizardData, type WizardData } from '../../types/gameSystem';

vi.mock('../../api/client', () => ({
  apiClient: { post: vi.fn(() => Promise.resolve({ data: { id: 'e1' } })) },
}));

function rules(): WizardData {
  const d = defaultWizardData();
  d.creationBudget = { ap: 100, attrBase: 8 };
  d.attributeCosts = { default: [{ upTo: 14, cost: 1 }] };
  d.attributes = [
    { name: 'mut', type: 'INT', min: 1, max: 20, default: 8 },
    { name: 'klugheit', type: 'INT', min: 1, max: 20, default: 8 },
  ];
  d.packages = [
    {
      name: 'Elf', kind: 'species', cost: 18,
      attributeMods: [{ attr: 'mut', value: 1 }, { choice: ['klugheit'], value: -1 }],
      autoTraits: ['Nachtsicht'],
    },
  ];
  d.traits = [{ name: 'Zauberer', kind: 'advantage', costs: [{ tier: 'I', cost: 25 }] }];
  d.advancement = { columns: ['A', 'B'], table: [{ from: 0, to: 20, costs: { A: 1, B: 2 } }], maxRule: '' };
  d.skills = [{ name: 'Klettern', attributes: ['mut'], bonus: 0, costColumn: 'B' }];
  return d;
}

describe('CharacterWizard (P30)', () => {
  it('Paketwahl und Attributkauf aktualisieren das Budget live', () => {
    render(<CharacterWizard worldId="w1" rules={rules()} onCreated={() => {}} onClose={() => {}} />);
    expect(screen.getByText('0 / 100 AP')).toBeInTheDocument();

    fireEvent.change(screen.getByRole('combobox', { name: /Spezies/ }), { target: { value: 'Elf' } });
    expect(screen.getByText('18 / 100 AP')).toBeInTheDocument();

    fireEvent.change(screen.getByRole('combobox', { name: /Elf Auswahl/ }), { target: { value: 'klugheit' } });
    fireEvent.click(screen.getByRole('button', { name: 'Weiter' }));
    fireEvent.click(screen.getByRole('button', { name: 'mut +' }));

    expect(screen.getByText('19 / 100 AP')).toBeInTheDocument();
  });

  it('speichert Endwerte + End-Traits als Entity', async () => {
    render(<CharacterWizard worldId="w1" rules={rules()} onCreated={() => {}} onClose={() => {}} />);

    fireEvent.change(screen.getByRole('combobox', { name: /Spezies/ }), { target: { value: 'Elf' } });
    fireEvent.change(screen.getByRole('combobox', { name: /Elf Auswahl/ }), { target: { value: 'klugheit' } });
    fireEvent.click(screen.getByRole('button', { name: 'Weiter' }));
    fireEvent.click(screen.getByRole('button', { name: 'mut +' }));
    fireEvent.click(screen.getByRole('button', { name: 'Weiter' }));
    fireEvent.click(screen.getByRole('checkbox', { name: /Zauberer/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Weiter' }));
    fireEvent.click(screen.getByRole('button', { name: 'Klettern +' }));
    fireEvent.click(screen.getByRole('button', { name: 'Klettern +' }));
    fireEvent.click(screen.getByRole('button', { name: 'Weiter' }));
    fireEvent.change(screen.getByLabelText(/Name/), { target: { value: 'Elaria' } });
    fireEvent.click(screen.getByRole('button', { name: 'Speichern' }));

    expect(apiClient.post).toHaveBeenCalledWith('/worlds/w1/entities', expect.objectContaining({
      entityType: 'PC',
      name: 'Elaria',
      attributesJson: JSON.stringify({ mut: 10, klugheit: 7 }),
      skillsJson: JSON.stringify({ Klettern: 2 }),
      metadataJson: JSON.stringify({
        traits: ['Zauberer', 'Nachtsicht'],
        package_selections: [{ name: 'Elf', choices: ['klugheit'] }],
      }),
    }));
  });
});
