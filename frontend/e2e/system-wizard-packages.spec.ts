import { test, expect } from '@playwright/test';
import { readFileSync } from 'node:fs';

const API = process.env.E2E_API_URL ?? 'http://localhost:8080';

function tokenFromState(): string {
  const state = JSON.parse(readFileSync('e2e/.auth/user.json', 'utf-8'));
  for (const origin of state.origins ?? []) {
    for (const item of origin.localStorage ?? []) {
      if (item.name === 'lwe:accessToken') return item.value as string;
    }
  }
  throw new Error('accessToken fehlt im Storage-State');
}

test.describe('SystemWizard — P29-T05 Pakete', () => {
  const name = `E2E PW ${Date.now()}`;
  let systemId = '';

  test.afterAll(async ({ request }) => {
    if (systemId) {
      await request.delete(`${API}/api/v1/game-systems/${systemId}`, {
        headers: { Authorization: `Bearer ${tokenFromState()}` },
      });
    }
  });

  test('zeigt Paket-Daten aus dem gespeicherten System und Live-Vorschau', async ({ page, request }) => {
    const rulesJson = JSON.stringify({
      version: 1,
      probeType: 'd20_target',
      attributes: [{ name: 'staerke', type: 'INT', min: 1, max: 20, default: 10 }],
      dice_mechanics: { probe: '1d20+mod' },
      packages: [
        {
          name: 'Elfe',
          kind: 'species',
          cost: 18,
          attributeMods: [{ attr: 'staerke', value: 1 }],
          autoTraits: ['Nachtsicht'],
          restricted: ['Zwerg'],
          recommended: ['Waldelf'],
        },
      ],
    });
    const created = await request.post(`${API}/api/v1/game-systems`, {
      headers: { Authorization: `Bearer ${tokenFromState()}` },
      data: { name, version: 1, rulesJson },
    });
    expect(created.ok(), `System anlegen fehlgeschlagen: ${created.status()}`).toBeTruthy();
    systemId = (await created.json()).id;

    await page.goto('/game-systems');
    await page.getByRole('button', { name: `Edit ${name}` }).click();

    await expect(page.getByRole('heading', { name: `Edit: ${name}` })).toBeVisible();

    const paketeHeading = page.getByRole('heading', { name: /Pakete \(Spezies/ });
    for (let i = 0; i < 14 && !(await paketeHeading.isVisible().catch(() => false)); i++) {
      await page.getByRole('button', { name: 'Weiter', exact: true }).click();
    }

    await expect(paketeHeading).toBeVisible();
    await expect(page.getByRole('textbox', { name: 'Name' })).toHaveValue('Elfe');
    await expect(page.getByRole('spinbutton', { name: 'Kosten' })).toHaveValue('18');
    await expect(page.getByRole('textbox', { name: 'Auto-Merkmale' })).toHaveValue('Nachtsicht');
    await expect(page.getByRole('textbox', { name: 'Eingeschränkt' })).toHaveValue('Zwerg');

    await page.getByRole('combobox', { name: 'Spezies' }).selectOption('Elfe');
    await expect(page.getByText(/Kosten: 18 AP · staerke \+1 · Nachtsicht/)).toBeVisible();

    await page.getByRole('button', { name: 'Weiter', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Übersicht & Speichern' })).toBeVisible();
    await page.getByRole('button', { name: 'System Speichern' }).click();
    await expect(page.getByRole('heading', { name: 'Game Systems' })).toBeVisible();
    await expect(page.getByRole('button', { name: `Edit ${name}` })).toBeVisible();
  });
});

test.describe('Auth-Smoke', () => {
  test('eingeloggt: Dashboard zeigt Weltliste', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(page.getByRole('heading', { name: 'Meine Welten' })).toBeVisible();
  });
});
