import { test, expect } from '@playwright/test';
import { IDS, API, ensureQaAuth, qaPage, tokenFor, waitVisible } from './qa-helpers';

/**
 * A11y-Gate: stellt sicher, dass Buttons einen zugänglichen Namen und Eingaben
 * ein Label (`label[for]`, `aria-label` oder `title`) haben. Läuft über die
 * Kernseiten und das NPC-Edit-Modal. Bei Befunden listet der Fehler die Elemente.
 */
test.describe.serial('A11y-Gate', () => {
  let regionId = '';
  let locationId = '';

  test.beforeAll(async ({ request }) => {
    await ensureQaAuth(request);
    const auth = { headers: { Authorization: `Bearer ${tokenFor('dm')}` } };
    const regions = await request.get(`${API}/api/v1/worlds/${IDS.forkWorldId}/regions`, auth);
    regionId = (await regions.json())[0].id;
    const locations = await request.get(`${API}/api/v1/regions/${regionId}/locations`, auth);
    locationId = (await locations.json())[0].id;
    expect(locationId).toBeTruthy();
  });

  test('keine unbenannten Buttons/Inputs auf den Kernseiten', async ({ browser }) => {
    const page = await qaPage(browser, 'dm');
    const routes: { name: string; path: string }[] = [
      { name: 'dashboard', path: '/dashboard' },
      { name: 'entities', path: `/worlds/${IDS.forkWorldId}/entities` },
      { name: 'systems', path: '/game-systems' },
      { name: 'location', path: `/worlds/${IDS.forkWorldId}/locations/${locationId}` },
      { name: 'market', path: `/worlds/${IDS.forkWorldId}/locations/${locationId}/market` },
      { name: 'sheet', path: `/characters/${IDS.miraId}` },
      { name: 'inventory', path: `/characters/${IDS.miraId}/inventory` },
      { name: 'npc', path: `/worlds/${IDS.forkWorldId}/npcs/${IDS.alrikId}` },
      { name: 'region', path: `/worlds/${IDS.forkWorldId}/regions/${regionId}` },
      { name: 'factions', path: `/worlds/${IDS.forkWorldId}/factions` },
      { name: 'campaign', path: `/campaigns/${IDS.campaignId}` },
      { name: 'world-edit', path: `/worlds/${IDS.forkWorldId}/edit` },
      { name: 'adventures', path: `/worlds/${IDS.forkWorldId}/adventures` },
      { name: 'map', path: `/worlds/${IDS.forkWorldId}/map` },
    ];
    const offenders: string[] = [];
    for (const route of routes) {
      await page.goto(route.path);
      await page.waitForLoadState('networkidle').catch(() => {});
      offenders.push(...(await scan(page, route.name)));
    }

    // NPC-Edit-Modal (dort liegen die Feld-Labels).
    await page.goto(`/worlds/${IDS.forkWorldId}/npcs/${IDS.alrikId}`);
    await waitVisible(page.getByRole('button', { name: /Edit/i }));
    await page.getByRole('button', { name: /Edit/i }).first().click();
    await page.waitForTimeout(400);
    offenders.push(...(await scan(page, 'npc-edit-modal')));

    await page.close();
    expect(offenders, `A11y-Befunde:\n${offenders.join('\n')}`).toEqual([]);
  });
});

async function scan(page: import('@playwright/test').Page, name: string): Promise<string[]> {
  return page.evaluate((pageName: string) => {
    const named = (el: Element) =>
      (el.textContent ?? '').trim().length > 0 ||
      el.getAttribute('aria-label') ||
      el.getAttribute('title');
    const out: string[] = [];
    for (const b of document.querySelectorAll('button')) {
      if (!named(b)) out.push(`${pageName} BUTTON ${b.outerHTML.slice(0, 120)}`);
    }
    for (const el of document.querySelectorAll('input,select,textarea')) {
      const id = (el as HTMLInputElement).id;
      if (
        !el.getAttribute('aria-label') &&
        !el.getAttribute('title') &&
        !(id && document.querySelector(`label[for="${id}"]`))
      ) {
        out.push(`${pageName} INPUT ${el.outerHTML.slice(0, 120)}`);
      }
    }
    return out;
  }, name);
}
