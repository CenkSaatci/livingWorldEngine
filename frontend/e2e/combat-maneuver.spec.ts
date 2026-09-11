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

test.describe('Kampf-E2E Manöver (P31-T02)', () => {
  const stamp = Date.now();
  let systemId = '';
  let campaignId = '';
  let worldId = '';
  let combatSessionId = '';
  const entityIds: string[] = [];

  test.afterAll(async ({ request }) => {
    if (combatSessionId) {
      await request.post(`${API}/api/v1/combat/${combatSessionId}/end`, { headers: auth() });
    }
    for (const id of entityIds) {
      if (id && worldId) {
        await request.delete(`${API}/api/v1/worlds/${worldId}/entities/${id}`, { headers: auth() });
      }
    }
    if (campaignId) await request.delete(`${API}/api/v1/campaigns/${campaignId}`, { headers: auth() });
    if (systemId) await request.delete(`${API}/api/v1/game-systems/${systemId}`, { headers: auth() });
  });

  test('startet Kampf im UI und führt Wuchtschlag aus (AP-Gate)', async ({ page, request }) => {
    const rulesJson = JSON.stringify({
      version: 1,
      probeType: 'd20_target',
      attributes: [{ name: 'staerke', type: 'INT', min: 1, max: 20, default: 10 }],
      dice_mechanics: {
        probe: '1d20+mod',
        combat: {
          initiative: '1d20',
          damage: '1d6',
          action_points: { max: 2 },
          action_types: ['action'],
          actions_per_turn: { action: 1 },
          maneuvers: [
            {
              name: 'Wuchtschlag', apCost: 2, damageType: 'bludgeoning',
              effects: [{ target: 'damage', op: 'add', value: 4 }],
            },
          ],
        },
      },
    });
    const sys = await request.post(`${API}/api/v1/game-systems`, {
      headers: auth(), data: { name: `E2E Kampf ${stamp}`, version: 1, rulesJson },
    });
    expect(sys.ok()).toBeTruthy();
    systemId = (await sys.json()).id;

    const worlds = await request.get(`${API}/api/v1/worlds`, { headers: auth() });
    const worldList = (await worlds.json()) as { id: string }[];
    expect(worldList.length, 'E2E braucht eine bestehende Welt (Limit 1)').toBeGreaterThan(0);
    const templateWorldId = worldList[0].id;

    const campaign = await request.post(`${API}/api/v1/campaigns`, {
      headers: auth(), data: { worldId: templateWorldId, gameSystemId: systemId, name: `E2E Kampf Runde ${stamp}` },
    });
    expect(campaign.ok()).toBeTruthy();
    const campaignBody = await campaign.json();
    campaignId = campaignBody.id;
    worldId = campaignBody.worldId; // Fork (P27-T03)

    for (const [name, type] of [[`E2E Kaempfer ${stamp}`, 'PC'], [`E2E Ork ${stamp}`, 'NPC']] as const) {
      const res = await request.post(`${API}/api/v1/worlds/${worldId}/entities`, {
        headers: auth(),
        data: { entityType: type, name, attributesJson: JSON.stringify({ staerke: 12 }) },
      });
      expect(res.ok()).toBeTruthy();
      entityIds.push((await res.json()).id);
    }

    await page.goto(`/campaigns/${campaignId}`);
    await expect(page.getByRole('heading', { name: `E2E Kampf Runde ${stamp}` })).toBeVisible();
    await page.getByRole('button', { name: 'In Welt starten' }).click();
    await expect(page).toHaveURL(new RegExp(`/worlds/${worldId}$`));

    await page.getByRole('button', { name: 'Kampf' }).click();
    await page.getByRole('checkbox', { name: new RegExp(`E2E Kaempfer ${stamp}`) }).check();
    await page.getByRole('checkbox', { name: new RegExp(`E2E Ork ${stamp}`) }).check();
    await page.getByRole('button', { name: 'Kampf starten', exact: true }).click();

    await expect(page.getByRole('button', { name: /Wuchtschlag/ })).toBeVisible({ timeout: 10_000 });

    // Ziel waehlen (ohne Ziel sind Angriffs-Buttons gesperrt) — der aktuelle
    // Actor wird aus der Zielliste ausgeschlossen, daher erster Eintrag.
    const targetSection = page.getByText('Ziel', { exact: true }).locator('..');
    await targetSection.getByRole('button').first().click();
    await expect(page.getByRole('button', { name: /Wuchtschlag/ })).toBeEnabled();

    // AP-Kosten 2 bei apMax 2: Response beweist die Ausfuehrung (Actor bei 0 AP)
    const [maneuverResponse] = await Promise.all([
      page.waitForResponse((r) => r.url().includes('/maneuver') && r.request().method() === 'POST'),
      page.getByRole('button', { name: /Wuchtschlag/ }).click(),
    ]);
    expect(maneuverResponse.ok()).toBeTruthy();
    const body = await maneuverResponse.json();
    combatSessionId = body.session.id;
    expect(body.participants.some((p: { apCurrent: number }) => p.apCurrent === 0)).toBe(true);
    await expect(page.getByRole('button', { name: /Wuchtschlag/ })).toBeDisabled({ timeout: 10_000 });
  });
});
