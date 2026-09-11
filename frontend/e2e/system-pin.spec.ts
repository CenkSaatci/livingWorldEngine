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

test.describe('System-Pin + Nachziehen (P27-T05 E2E)', () => {
  const stamp = Date.now();
  let systemId = '';
  let campaignId = '';

  test.afterAll(async ({ request }) => {
    if (campaignId) await request.delete(`${API}/api/v1/campaigns/${campaignId}`, { headers: auth() });
    if (systemId) await request.delete(`${API}/api/v1/game-systems/${systemId}`, { headers: auth() });
  });

  test('pinnt die Version und zieht sie per UI nach', async ({ page, request }) => {
    const rulesV1 = JSON.stringify({
      version: 1,
      probeType: 'd20_target',
      attributes: [{ name: 'staerke', type: 'INT', min: 1, max: 20, default: 10 }],
      dice_mechanics: { probe: '1d20+mod' },
    });
    const sys = await request.post(`${API}/api/v1/game-systems`, {
      headers: auth(), data: { name: `E2E Pin ${stamp}`, version: 1, rulesJson: rulesV1 },
    });
    expect(sys.ok()).toBeTruthy();
    systemId = (await sys.json()).id;

    const worlds = await request.get(`${API}/api/v1/worlds`, { headers: auth() });
    const templateWorldId = ((await worlds.json()) as { id: string }[])[0].id;
    const campaign = await request.post(`${API}/api/v1/campaigns`, {
      headers: auth(),
      data: { worldId: templateWorldId, gameSystemId: systemId, name: `E2E Pin Runde ${stamp}` },
    });
    expect(campaign.ok()).toBeTruthy();
    campaignId = (await campaign.json()).id;

    // System-Regeln aendern -> Version bumpt automatisch; Kampagne bleibt gepinnt
    const rulesV2 = rulesV1.replace('"default":10', '"default":12');
    const patch = await request.patch(`${API}/api/v1/game-systems/${systemId}`, {
      headers: auth(), data: { name: `E2E Pin ${stamp}`, version: 1, rulesJson: rulesV2 },
    });
    expect(patch.ok()).toBeTruthy();
    expect((await patch.json()).version).toBe(2);
    const pinned = await request.get(`${API}/api/v1/campaigns/${campaignId}`, { headers: auth() });
    expect((await pinned.json()).gameSystemVersion).toBe(1);

    await page.goto(`/campaigns/${campaignId}`);
    await expect(page.getByRole('heading', { name: `E2E Pin Runde ${stamp}` })).toBeVisible();
    await expect(page.getByText(/System-Update verfügbar/)).toBeVisible({ timeout: 10_000 });

    await page.getByRole('button', { name: 'System aktualisieren' }).click();
    await expect(page.getByText(/System-Update verfügbar/)).toHaveCount(0, { timeout: 10_000 });

    const after = await request.get(`${API}/api/v1/campaigns/${campaignId}`, { headers: auth() });
    expect((await after.json()).gameSystemVersion).toBe(2);
  });
});
