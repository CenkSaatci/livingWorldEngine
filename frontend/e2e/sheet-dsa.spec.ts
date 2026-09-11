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

test.describe('Sheet-E2E DSA-Referenz (P31-T01)', () => {
  const stamp = Date.now();
  let systemId = '';
  let campaignId = '';
  let worldId = '';
  let heroId = '';
  let plainId = '';

  test.afterAll(async ({ request }) => {
    for (const id of [heroId, plainId]) {
      if (id && worldId) {
        await request.delete(`${API}/api/v1/worlds/${worldId}/entities/${id}`, { headers: auth() });
      }
    }
    if (campaignId) await request.delete(`${API}/api/v1/campaigns/${campaignId}`, { headers: auth() });
    if (systemId) await request.delete(`${API}/api/v1/game-systems/${systemId}`, { headers: auth() });
  });

  test('zeigt sk-Tabelle, asp nur mit Zauberer und Rüstung', async ({ page, request }) => {
    const rulesJson = readFileSync(new URL('../../docs/examples/dsa5.json', import.meta.url), 'utf-8');
    const sys = await request.post(`${API}/api/v1/game-systems`, {
      headers: auth(), data: { name: `E2E DSA ${stamp}`, version: 1, rulesJson },
    });
    expect(sys.ok()).toBeTruthy();
    systemId = (await sys.json()).id;

    const worlds = await request.get(`${API}/api/v1/worlds`, { headers: auth() });
    const worldList = (await worlds.json()) as { id: string }[];
    expect(worldList.length, 'E2E braucht eine bestehende Welt (Limit 1)').toBeGreaterThan(0);
    const templateWorldId = worldList[0].id;

    const campaign = await request.post(`${API}/api/v1/campaigns`, {
      headers: auth(), data: { worldId: templateWorldId, gameSystemId: systemId, name: `E2E DSA Runde ${stamp}` },
    });
    expect(campaign.ok()).toBeTruthy();
    const campaignBody = await campaign.json();
    campaignId = campaignBody.id;
    worldId = campaignBody.worldId; // Fork (P27-T03)

    const attrs = JSON.stringify({
      mut: 14, klugheit: 13, intuition: 12, konstitution: 12, koerperkraft: 12,
    });
    const hero = await request.post(`${API}/api/v1/worlds/${worldId}/entities`, {
      headers: auth(),
      data: {
        entityType: 'PC',
        name: `E2E DSA Held ${stamp}`,
        attributesJson: attrs,
        metadataJson: JSON.stringify({ traits: ['Zauberer'], damage_armor: 3 }),
      },
    });
    expect(hero.ok()).toBeTruthy();
    heroId = (await hero.json()).id;

    const plain = await request.post(`${API}/api/v1/worlds/${worldId}/entities`, {
      headers: auth(),
      data: { entityType: 'PC', name: `E2E DSA Barde ${stamp}`, attributesJson: attrs, metadataJson: '{}' },
    });
    expect(plain.ok()).toBeTruthy();
    plainId = (await plain.json()).id;

    // Aktive Kampagne setzen (persistiert) und Sheet öffnen
    await page.goto(`/campaigns/${campaignId}`);
    await expect(page.getByRole('heading', { name: `E2E DSA Runde ${stamp}` })).toBeVisible();
    await page.goto(`/characters/${heroId}`);

    await expect(page.getByRole('heading', { name: `E2E DSA Held ${stamp}` })).toBeVisible({ timeout: 10_000 });
    // sk: MU14+KL13+IN12 = 39 -> Tabelle 39-44 -> 7
    await expect(page.getByText('sk', { exact: true }).locator('xpath=..')).toContainText('7');
    // asp: (14+13+12)/2 = 19.5, nur mit Trait Zauberer
    await expect(page.getByText('asp', { exact: true }).locator('xpath=..')).toContainText('19.5');
    // Rüstung aus metadata.damage_armor
    await expect(page.locator('[title="Rüstung"]')).toContainText('3');

    // Ohne Zauberer-Trait fehlt asp; ohne Rüstung kein Badge
    await page.goto(`/characters/${plainId}`);
    await expect(page.getByRole('heading', { name: `E2E DSA Barde ${stamp}` })).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText('sk', { exact: true }).locator('xpath=..')).toContainText('7');
    await expect(page.getByText('asp', { exact: true })).toHaveCount(0);
    await expect(page.locator('[title="Rüstung"]')).toHaveCount(0);
  });
});
