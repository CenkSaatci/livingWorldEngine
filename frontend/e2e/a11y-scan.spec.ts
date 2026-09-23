import { test, expect } from '@playwright/test';
import { IDS, API, ensureQaAuth, qaPage, tokenFor, waitVisible } from './qa-helpers';

/**
 * Diagnose (nicht Teil der Suite): listet Buttons ohne zugänglichen Namen und
 * Eingaben ohne Label-Verknüpfung je Seite auf. Grundlage für den A11y-Fix.
 */
test.describe.serial('A11y-Scan', () => {
  let locationId = '';

  test.beforeAll(async ({ request }) => {
    await ensureQaAuth(request);
    const auth = { headers: { Authorization: `Bearer ${tokenFor('dm')}` } };
    const regions = await request.get(`${API}/api/v1/worlds/${IDS.forkWorldId}/regions`, auth);
    const regionId = (await regions.json())[0].id;
    const locations = await request.get(`${API}/api/v1/regions/${regionId}/locations`, auth);
    locationId = (await locations.json())[0].id;
    expect(locationId).toBeTruthy();
  });

  test('scan pages', async ({ browser }) => {
    const page = await qaPage(browser, 'dm');
    const routes: { name: string; path: string }[] = [
      { name: 'dashboard', path: '/dashboard' },
      { name: 'entities', path: `/worlds/${IDS.forkWorldId}/entities` },
      { name: 'systems', path: '/game-systems' },
      { name: 'location', path: `/worlds/${IDS.forkWorldId}/locations/${locationId}` },
      { name: 'market', path: `/worlds/${IDS.forkWorldId}/locations/${locationId}/market` },
      { name: 'sheet', path: `/characters/${IDS.miraId}` },
      { name: 'npc', path: `/worlds/${IDS.forkWorldId}/npcs/${IDS.alrikId}` },
      { name: 'map', path: `/worlds/${IDS.forkWorldId}/map` },
    ];
    for (const route of routes) {
      await scan(page, route.name, route.path);
    }

    // NPC-Edit-Modal separat (dort liegen die 13 unverknüpften Felder).
    await page.goto(`/worlds/${IDS.forkWorldId}/npcs/${IDS.alrikId}`);
    await waitVisible(page.getByRole('button', { name: /Edit/i }));
    await page.getByRole('button', { name: /Edit/i }).first().click();
    await page.waitForTimeout(500);
    await scanCurrent(page, 'npc-edit-modal');

    await page.close();
  });
});

async function scan(page: import('@playwright/test').Page, name: string, path: string) {
  await page.goto(path);
  await page.waitForLoadState('networkidle').catch(() => {});
  await scanCurrent(page, name);
}

async function scanCurrent(page: import('@playwright/test').Page, name: string) {
  const report = await page.evaluate(() => {
    const named = (el: Element) =>
      (el.textContent ?? '').trim().length > 0 ||
      el.getAttribute('aria-label') ||
      el.getAttribute('title');
    const buttons = [...document.querySelectorAll('button')]
      .filter((b) => !named(b))
      .map((b) => b.outerHTML.slice(0, 200));
    const inputs = [...document.querySelectorAll('input,select,textarea')]
      .filter((el) => {
        const id = (el as HTMLInputElement).id;
        return (
          !el.getAttribute('aria-label') &&
          !el.getAttribute('title') &&
          !(id && document.querySelector(`label[for="${id}"]`))
        );
      })
      .map((el) => el.outerHTML.slice(0, 200));
    return { buttons, inputs };
  });
  // eslint-disable-next-line no-console
  console.log(`\n### ${name}  (buttons=${report.buttons.length}, inputs=${report.inputs.length})`);
  for (const b of report.buttons) console.log(`  BUTTON  ${b}`);
  for (const i of report.inputs) console.log(`  INPUT   ${i}`);
}
