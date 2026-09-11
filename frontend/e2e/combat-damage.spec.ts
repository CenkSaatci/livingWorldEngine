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

const auth = () => ({ Authorization: `Bearer ${tokenFromState()}` });

test.describe('Kampf-Zustände + Schadensart (T33-01)', () => {
  const stamp = Date.now();
  let systemId = '';
  let campaignId = '';
  let worldId = '';
  const entityIds: string[] = [];

  test.afterAll(async ({ request }) => {
    for (const id of entityIds) {
      if (id && worldId) {
        await request.delete(`${API}/api/v1/worlds/${worldId}/entities/${id}`, { headers: auth() });
      }
    }
    if (campaignId) await request.delete(`${API}/api/v1/campaigns/${campaignId}`, { headers: auth() });
    if (systemId) await request.delete(`${API}/api/v1/game-systems/${systemId}`, { headers: auth() });
  });

  test('Feuer-Waffe + Rüstung im Log, Zustand tickt nach Runden', async ({ page, request }) => {
    const rulesJson = JSON.stringify({
      version: 1,
      probeType: 'd20_target',
      attributes: [{ name: 'staerke', type: 'INT', min: 1, max: 20, default: 10 }],
      conditions: [
        { name: 'Wunde', rounds: 2, effects: [{ target: 'probe', op: 'add', value: -4 }] },
      ],
      dice_mechanics: {
        probe: '1d20+mod',
        combat: {
          initiative: '1d20',
          damage: '1d8',
          action_points: { max: 2 },
          action_types: ['action'],
          actions_per_turn: { action: 1 },
        },
      },
    });
    const sys = await request.post(`${API}/api/v1/game-systems`, {
      headers: auth(), data: { name: `E2E Dmg ${stamp}`, version: 1, rulesJson },
    });
    expect(sys.ok()).toBeTruthy();
    systemId = (await sys.json()).id;

    // Feuer-Waffe am System
    const item = await request.post(`${API}/api/v1/game-systems/${systemId}/items`, {
      headers: auth(),
      data: {
        name: `Fackel ${stamp}`, type: 'WEAPON', weight: 1, value: 5,
        metadataJson: JSON.stringify({ damage_type: 'fire' }),
      },
    });
    expect(item.ok()).toBeTruthy();
    const itemId = (await item.json()).id;

    const worlds = await request.get(`${API}/api/v1/worlds`, { headers: auth() });
    const templateWorldId = ((await worlds.json()) as { id: string }[])[0].id;
    const campaign = await request.post(`${API}/api/v1/campaigns`, {
      headers: auth(),
      data: { worldId: templateWorldId, gameSystemId: systemId, name: `E2E Dmg Runde ${stamp}` },
    });
    expect(campaign.ok()).toBeTruthy();
    const campaignBody = await campaign.json();
    campaignId = campaignBody.id;
    worldId = campaignBody.worldId; // Fork

    for (const name of [`E2E Held ${stamp}`, `E2E Golem ${stamp}`]) {
      const res = await request.post(`${API}/api/v1/worlds/${worldId}/entities`, {
        headers: auth(),
        data: { entityType: 'PC', name, attributesJson: JSON.stringify({ staerke: 12 }) },
      });
      expect(res.ok()).toBeTruthy();
      entityIds.push((await res.json()).id);
    }
    const [heroId, golemId] = entityIds;

    // Waffe in beide Inventare + ausrüsten
    for (const entityId of [heroId, golemId]) {
      await request.post(`${API}/api/v1/entities/${entityId}/inventory/add`, {
        headers: auth(), data: { itemId, quantity: 1 },
      });
      const equip = await request.post(`${API}/api/v1/entities/${entityId}/inventory/equip`, {
        headers: auth(), data: { itemId, slot: 'weapon' },
      });
      expect(equip.ok()).toBeTruthy();
    }
    // BEIDE: Ruestung 100 → deterministisch 0 Schaden, unabhaengig von der Initiative-Reihenfolge
    for (const entityId of [heroId, golemId]) {
      const patch = await request.patch(`${API}/api/v1/worlds/${worldId}/entities/${entityId}`, {
        headers: auth(), data: { metadataJson: JSON.stringify({ damage_armor: 100 }) },
      });
      expect(patch.ok()).toBeTruthy();
    }

    // Kampf im UI starten und Start-Response abfangen (Session + Teilnehmer)
    await page.goto(`/campaigns/${campaignId}`);
    await expect(page.getByRole('heading', { name: `E2E Dmg Runde ${stamp}` })).toBeVisible();
    await page.getByRole('button', { name: 'In Welt starten' }).click();
    await expect(page).toHaveURL(new RegExp(`/worlds/${worldId}$`));

    await page.getByRole('button', { name: 'Kampf' }).click();
    for (const name of [`E2E Held ${stamp}`, `E2E Golem ${stamp}`]) {
      await page.getByRole('checkbox', { name: new RegExp(name) }).check();
    }
    const [startResponse] = await Promise.all([
      page.waitForResponse((r) => r.url().includes('/combat/start') && r.request().method() === 'POST'),
      page.getByRole('button', { name: 'Kampf starten', exact: true }).click(),
    ]);
    expect(startResponse.ok()).toBeTruthy();
    const startBody = await startResponse.json();
    const current = startBody.currentTurnEntityId as string;
    const targetId = [heroId, golemId].find((id) => id !== current)!;
    const targetName = [heroId, golemId].indexOf(targetId) === 0
      ? `E2E Held ${stamp}` : `E2E Golem ${stamp}`;

    // Angriff: Ziel waehlen, Aktion ausfuehren
    await expect(page.getByRole('button', { name: /action/i })).toBeVisible({ timeout: 10_000 });
    await page.getByRole('button', { name: new RegExp(targetName) }).first().click({ force: true });
    await page.getByRole('button', { name: /action/i }).click();

    // Chat-Log: Schadensart + deterministisch 0 Schaden (Ruestung 100)
    await expect(page.getByText(/\(fire\)/)).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText(/0 Schaden/)).toBeVisible();

    // Zustand auf das Ziel (DM) → Sheet zeigt ihn
    const addCondition = await request.post(
      `${API}/api/v1/entities/${targetId}/conditions?campaignId=${campaignId}`,
      { headers: auth(), data: { name: 'Wunde', rounds: 1 } },
    );
    expect(addCondition.ok()).toBeTruthy();
    const sheetBefore = await request.get(
      `${API}/api/v1/entities/${targetId}/sheet?campaignId=${campaignId}`,
      { headers: auth() },
    );
    expect((await sheetBefore.json()).activeConditions).toHaveLength(1);

    // Naechster Zug → Ziel beginnt seinen Zug → rounds=1 tickt ab
    await page.getByRole('button', { name: /Nächster Zug/ }).click();
    await expect
      .poll(async () => {
        const res = await request.get(
          `${API}/api/v1/entities/${targetId}/sheet?campaignId=${campaignId}`,
          { headers: auth() },
        );
        return ((await res.json()).activeConditions as unknown[]).length;
      }, { timeout: 10_000 })
      .toBe(0);
  });
});
