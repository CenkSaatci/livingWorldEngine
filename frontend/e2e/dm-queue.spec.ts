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

test.describe('DM-Queue Live + Bulk (T33-08)', () => {
  const stamp = Date.now();
  let systemId = '';
  let campaignId = '';
  let worldId = '';
  let npcId = '';

  test.afterAll(async ({ request }) => {
    if (npcId && worldId) {
      await request.delete(`${API}/api/v1/worlds/${worldId}/entities/${npcId}`, { headers: auth() });
    }
    if (campaignId) await request.delete(`${API}/api/v1/campaigns/${campaignId}`, { headers: auth() });
    if (systemId) await request.delete(`${API}/api/v1/game-systems/${systemId}`, { headers: auth() });
  });

  test('Panel erscheint per WS und Bulk-Approve leert die Queue', async ({ page, request }) => {
    const rulesJson = JSON.stringify({
      version: 1,
      probeType: 'd20_target',
      attributes: [{ name: 'staerke', type: 'INT', min: 1, max: 20, default: 10 }],
      dice_mechanics: { probe: '1d20+mod' },
    });
    const sys = await request.post(`${API}/api/v1/game-systems`, {
      headers: auth(), data: { name: `E2E Queue ${stamp}`, version: 1, rulesJson },
    });
    expect(sys.ok()).toBeTruthy();
    systemId = (await sys.json()).id;

    const worlds = await request.get(`${API}/api/v1/worlds`, { headers: auth() });
    const templateWorldId = ((await worlds.json()) as { id: string }[])[0].id;
    const campaign = await request.post(`${API}/api/v1/campaigns`, {
      headers: auth(),
      data: { worldId: templateWorldId, gameSystemId: systemId, name: `E2E Queue Runde ${stamp}` },
    });
    expect(campaign.ok()).toBeTruthy();
    const campaignBody = await campaign.json();
    campaignId = campaignBody.id;
    worldId = campaignBody.worldId;

    // Welt auf suggest (sonst lehnt der Intent-Service ab)
    const patchWorld = await request.patch(`${API}/api/v1/worlds/${worldId}`, {
      headers: auth(), data: { settingsJson: JSON.stringify({ ai_mode: 'suggest' }) },
    });
    expect(patchWorld.ok()).toBeTruthy();

    const npc = await request.post(`${API}/api/v1/worlds/${worldId}/entities`, {
      headers: auth(),
      data: { entityType: 'NPC', name: `E2E Wirt ${stamp}` },
    });
    expect(npc.ok()).toBeTruthy();
    npcId = (await npc.json()).id;

    // GameView oeffnen — Queue ist zunaechst leer
    await page.goto(`/campaigns/${campaignId}`);
    await expect(page.getByRole('heading', { name: `E2E Queue Runde ${stamp}` })).toBeVisible();
    await page.getByRole('button', { name: 'In Welt starten' }).click();
    await expect(page).toHaveURL(new RegExp(`/worlds/${worldId}$`));
    await expect(page.getByText('KI-Intent-Queue')).toHaveCount(0);

    // Intent per API anlegen → Panel soll ohne Reload erscheinen (WS, Poll-Fallback 5s)
    const intent = await request.post(`${API}/api/v1/npc-intents`, {
      headers: auth(),
      data: {
        worldId, campaignId, npcId, intentType: 'SPEAK',
        paramsJson: '{}', reasoning: 'Der Wirt grüßt.',
      },
    });
    expect(intent.ok()).toBeTruthy();

    await expect(page.getByText('KI-Intent-Queue')).toBeVisible({ timeout: 5_000 });
    await expect(page.getByText('Der Wirt grüßt.')).toBeVisible();

    // Bulk-Approve ueber die Checkbox
    await page.getByLabel(/^Select /).first().check();
    await page.getByRole('button', { name: /Approve selected/ }).click();

    await expect(page.getByText('KI-Intent-Queue')).toHaveCount(0, { timeout: 10_000 });
  });
});
