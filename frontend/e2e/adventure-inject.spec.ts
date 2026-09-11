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

test.describe('Adventure Inject-Choice (T33-09)', () => {
  const stamp = Date.now();
  let systemId = '';
  let campaignId = '';
  let worldId = '';
  let adventureId = '';

  test.afterAll(async ({ request }) => {
    if (campaignId) await request.delete(`${API}/api/v1/campaigns/${campaignId}`, { headers: auth() });
    if (systemId) await request.delete(`${API}/api/v1/game-systems/${systemId}`, { headers: auth() });
  });

  test('DM injiziert eine Choice live ins laufende Adventure', async ({ page, request }) => {
    const rulesJson = JSON.stringify({
      version: 1,
      probeType: 'd20_target',
      attributes: [{ name: 'staerke', type: 'INT', min: 1, max: 20, default: 10 }],
      dice_mechanics: { probe: '1d20+mod' },
    });
    const sys = await request.post(`${API}/api/v1/game-systems`, {
      headers: auth(), data: { name: `E2E Adv ${stamp}`, version: 1, rulesJson },
    });
    systemId = (await sys.json()).id;

    const worlds = await request.get(`${API}/api/v1/worlds`, { headers: auth() });
    const templateWorldId = ((await worlds.json()) as { id: string }[])[0].id;
    const campaign = await request.post(`${API}/api/v1/campaigns`, {
      headers: auth(),
      data: { worldId: templateWorldId, gameSystemId: systemId, name: `E2E Adv Runde ${stamp}` },
    });
    const campaignBody = await campaign.json();
    campaignId = campaignBody.id;
    worldId = campaignBody.worldId;

    const adv = await request.post(`${API}/api/v1/adventures`, {
      headers: auth(),
      data: { worldId, name: `Höhle ${stamp}` },
    });
    expect(adv.ok()).toBeTruthy();
    adventureId = (await adv.json()).id;
    const n1 = await request.post(`${API}/api/v1/adventures/${adventureId}/nodes`, {
      headers: auth(), data: { text: `Start ${stamp}`, isEnd: false },
    });
    const n2 = await request.post(`${API}/api/v1/adventures/${adventureId}/nodes`, {
      headers: auth(), data: { text: `Ziel ${stamp}`, isEnd: true },
    });
    const node1 = (await n1.json()).id as string;
    const node2 = (await n2.json()).id as string;

    await page.goto(`/campaigns/${campaignId}`);
    await expect(page.getByRole('heading', { name: `E2E Adv Runde ${stamp}` })).toBeVisible();
    await page.getByRole('button', { name: 'In Welt starten' }).click();
    await expect(page).toHaveURL(new RegExp(`/worlds/${worldId}$`));

    // Adventure im DM-Panel öffnen → Nodes laden
    await page.getByRole('button', { name: new RegExp(`Höhle ${stamp}`) }).click();
    await expect(page.getByText('Inject Choice:')).toBeVisible({ timeout: 10_000 });

    await page.getByPlaceholder('Choice label...').fill(`Geheimtür ${stamp}`);
    await page.getByLabel('Source node').selectOption(node1);
    await page.getByLabel('Target node').selectOption(node2);
    await page.getByRole('button', { name: 'Inject Choice' }).click();

    // API-Verifikation: Choice haengt am Quell-Node mit korrektem Ziel
    await expect
      .poll(async () => {
        const res = await request.get(
          `${API}/api/v1/adventures/${adventureId}/nodes/${node1}/choices`,
          { headers: auth() },
        );
        const choices = (await res.json()) as { label?: string; targetNodeId?: string }[];
        return choices.some((c) => c.label === `Geheimtür ${stamp}` && c.targetNodeId === node2);
      }, { timeout: 10_000 })
      .toBe(true);
  });
});
