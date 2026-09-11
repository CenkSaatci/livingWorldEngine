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

test.describe('Charakter-Wizard (P30-T04)', () => {
  const stamp = Date.now();
  const sysName = `E2E CW Sys ${stamp}`;
  const campaignName = `E2E CW Runde ${stamp}`;
  let systemId = '';
  let campaignId = '';

  test.afterAll(async ({ request }) => {
    if (campaignId) await request.delete(`${API}/api/v1/campaigns/${campaignId}`, { headers: auth() });
    if (systemId) await request.delete(`${API}/api/v1/game-systems/${systemId}`, { headers: auth() });
  });

  test('erstellt einen Elfen-PC mit Paket-Mods, Traits und speichert ihn', async ({ page, request }) => {
    const rulesJson = JSON.stringify({
      version: 1,
      probeType: 'd20_target',
      creationBudget: { ap: 100, attrBase: 8, fatePoints: 3 },
      attributeCosts: { default: [{ upTo: 14, cost: 1 }, { upTo: 19, cost: 2 }] },
      attributes: [
        { name: 'mut', type: 'INT', min: 1, max: 20, default: 8 },
        { name: 'klugheit', type: 'INT', min: 1, max: 20, default: 8 },
      ],
      dice_mechanics: { probe: '1d20+mod' },
      packages: [
        {
          name: 'Elf', kind: 'species', cost: 18,
          attributeMods: [{ attr: 'mut', value: 1 }, { choice: ['klugheit'], value: -1 }],
          autoTraits: ['Nachtsicht'],
        },
      ],
      traits: [{ name: 'Zauberer', kind: 'advantage', costs: [{ tier: 'I', cost: 25 }] }],
    });
    const sys = await request.post(`${API}/api/v1/game-systems`, {
      headers: auth(), data: { name: sysName, version: 1, rulesJson },
    });
    expect(sys.ok()).toBeTruthy();
    systemId = (await sys.json()).id;

    // Welt-Limit (1) ist erreicht: bestehende Welt des Users wiederverwenden.
    const worlds = await request.get(`${API}/api/v1/worlds`, { headers: auth() });
    expect(worlds.ok()).toBeTruthy();
    const worldList = (await worlds.json()) as { id: string; name: string }[];
    expect(worldList.length).toBeGreaterThan(0);
    const worldId = worldList[0].id;

    const campaign = await request.post(`${API}/api/v1/campaigns`, {
      headers: auth(), data: { worldId, gameSystemId: systemId, name: campaignName },
    });
    expect(campaign.ok()).toBeTruthy();
    campaignId = (await campaign.json()).id;

    // Aktive Kampagne setzen (CampaignDetail cachet sie im Store), dann Welt-Entities öffnen
    await page.goto(`/campaigns/${campaignId}`);
    await expect(page.getByRole('heading', { name: campaignName })).toBeVisible({ timeout: 10_000 });
    // "In Welt starten" persistiert die aktive Kampagne (campaignStore) — Reload-safe.
    await page.getByRole('button', { name: 'In Welt starten' }).click();
    await expect(page).toHaveURL(new RegExp(`/worlds/${worldId}`));
    await page.goto(`/worlds/${worldId}/entities`);

    await page.getByRole('button', { name: 'Charakter-Wizard' }).click();
    await expect(page.getByRole('heading', { name: 'Charakter erstellen' })).toBeVisible();

    await page.getByRole('combobox', { name: /Spezies/ }).selectOption('Elf');
    await page.getByRole('combobox', { name: /Elf Auswahl/ }).selectOption('klugheit');
    await page.getByRole('button', { name: 'Weiter' }).click();

    await page.getByRole('button', { name: 'mut +' }).click();
    await page.getByRole('button', { name: 'Weiter' }).click();

    await page.getByRole('checkbox', { name: /Zauberer/ }).check();
    await page.getByRole('button', { name: 'Weiter' }).click();

    await page.getByLabel(/Name/).fill('E2E Held');
    await page.getByRole('button', { name: 'Speichern' }).click();

    await expect(page).toHaveURL(/\/characters\/[0-9a-f-]+$/, { timeout: 10_000 });
    await expect(page.getByRole('heading', { name: 'E2E Held' })).toBeVisible();

    const entityId = page.url().split('/').pop()!;
    const sheet = await request.get(
      `${API}/api/v1/entities/${entityId}/sheet?campaignId=${campaignId}`,
      { headers: auth() },
    );
    expect(sheet.ok()).toBeTruthy();
    const body = await sheet.json();
    const attrs = Object.fromEntries(
      (body.attributes as { name: string; value: number }[]).map((a) => [a.name, a.value]),
    );
    expect(attrs.mut).toBe(10);      // 9 gekauft + 1 Elf
    expect(attrs.klugheit).toBe(7);  // 8 - 1 Elf-Choice
    expect(body.fatePoints).toBe(3);
  });
});
