import { test, expect } from '@playwright/test';
import {
  IDS, API, ensureQaAuth, qaPage, tokenFor, watchPage, summarizeWatch,
  heuristics, reportHeuristics, shot, note, check, saveQaResults,
  waitVisible, anyVisible,
} from './qa-helpers';

/**
 * Audit (ADR-015): UI/UX-Sicht auf die neuen POI-Oberflächen.
 * Läuft gegen die QA-Welt (unverändert) — die leeren Zustände sind Teil der Prüfung.
 */
test.describe.serial('POI-Audit', () => {
  let regionId = '';
  let locationId = '';
  let locationName = '';

  test.beforeAll(async ({ request }) => {
    await ensureQaAuth(request);
    const token = tokenFor('dm');
    const auth = { headers: { Authorization: `Bearer ${token}` } };

    const regions = await request.get(`${API}/api/v1/worlds/${IDS.forkWorldId}/regions`, auth);
    expect(regions.ok(), `Regionen laden: ${regions.status()}`).toBeTruthy();
    const region = (await regions.json())[0];
    regionId = region.id;

    const locations = await request.get(`${API}/api/v1/regions/${regionId}/locations`, auth);
    expect(locations.ok(), `Orte laden: ${locations.status()}`).toBeTruthy();
    const location = (await locations.json())[0];
    locationId = location.id;
    locationName = location.name;
    note('poi-audit', 'setup', 'INFO', `Region ${region.name}, Ort ${locationName} (${locationId})`);
  });

  test.afterAll(() => saveQaResults('poi-audit'));

  test('Ortsansicht: Aktions-Panel', async ({ browser }) => {
    const page = await qaPage(browser, 'dm');
    const w = watchPage(page);
    await page.goto(`/worlds/${IDS.forkWorldId}/locations/${locationId}`);
    await waitVisible(page.getByText('Aktionen'));

    check('poi-location', 'Panel', await anyVisible(page.getByText('Aktionen')), 'Aktions-Panel fehlt');
    check('poi-location', 'Charakterauswahl',
      await anyVisible(page.getByText('Charakter')), 'Actor-Select fehlt');
    // useApiGet braucht einen Moment — auf den Leerzustand warten statt sofort prüfen.
    const empty = page.getByText('Keine Aktionen an diesem Ort.');
    await waitVisible(empty);
    check('poi-location', 'Leerzustand', await anyVisible(empty), 'Leerzustand-Text fehlt');

    const shotPath = await shot(page, 'poi-location');
    reportHeuristics('poi-location', 'Ortsansicht', await heuristics(page), shotPath);
    summarizeWatch('poi-location', 'Ortsansicht', w, shotPath);
    await page.close();
  });

  test('Markt: Händler-Panel', async ({ browser }) => {
    const page = await qaPage(browser, 'dm');
    const w = watchPage(page);
    await page.goto(`/worlds/${IDS.forkWorldId}/locations/${locationId}/market`);
    await waitVisible(page.getByText('Markt'));

    // useApiGet braucht einen Moment — auf den Leerzustand warten statt sofort prüfen.
    const empty = page.getByText('Keine Händler an diesem Ort.');
    await waitVisible(empty);
    check('poi-market', 'Leerzustand', await anyVisible(empty), 'Händler-Leerzustand fehlt');
    check('poi-market', 'Charakterauswahl',
      await anyVisible(page.getByText('Charakter')), 'Actor-Select im Markt fehlt');

    const shotPath = await shot(page, 'poi-market');
    reportHeuristics('poi-market', 'Markt', await heuristics(page), shotPath);
    summarizeWatch('poi-market', 'Markt', w, shotPath);
    await page.close();
  });

  test('Charakterbogen: Geldzeile', async ({ browser }) => {
    const page = await qaPage(browser, 'dm');
    const w = watchPage(page);
    await page.goto(`/characters/${IDS.miraId}`);
    const moneyRow = page.locator('[title="Geld"]').first();
    await waitVisible(moneyRow);

    check('poi-sheet', 'Geldzeile', await anyVisible(moneyRow), 'Geldzeile fehlt');
    note('poi-sheet', 'Geldwert', 'INFO', `Anzeige: ${(await moneyRow.textContent().catch(() => ''))?.trim()}`);

    const shotPath = await shot(page, 'poi-sheet');
    reportHeuristics('poi-sheet', 'Bogen', await heuristics(page), shotPath);
    summarizeWatch('poi-sheet', 'Bogen', w, shotPath);
    await page.close();
  });

  test('NPC-Editor: Händler-Felder', async ({ browser }) => {
    const page = await qaPage(browser, 'dm');
    const w = watchPage(page);
    await page.goto(`/worlds/${IDS.forkWorldId}/npcs/${IDS.alrikId}`);
    await waitVisible(page.getByRole('button', { name: /Edit/i }));

    await page.getByRole('button', { name: /Edit/i }).first().click();
    const merchantToggle = page.getByText('Händler (Sortiment)');
    check('poi-npc', 'Händler-Schalter',
      await waitVisible(merchantToggle), 'Händler-Schalter im NPC-Editor fehlt');

    if (await anyVisible(merchantToggle)) {
      // Checkbox direkt per DOM-Click schalten (force-Click verfehlt das Off-Screen-Element).
      const box = page
        .locator('label', { hasText: 'Händler (Sortiment)' })
        .locator('input[type="checkbox"]')
        .first();
      await box.scrollIntoViewIfNeeded().catch(() => {});
      await box.evaluate((el) => (el as HTMLInputElement).click()).catch(() => {});
      check('poi-npc', 'Sortiment-Feld',
        await waitVisible(page.getByPlaceholder('Heiltrank'), 5000), 'Sortiment-Textarea fehlt');
    }

    const shotPath = await shot(page, 'poi-npc-edit');
    reportHeuristics('poi-npc', 'NPC-Editor', await heuristics(page), shotPath);
    summarizeWatch('poi-npc', 'NPC-Editor', w, shotPath);
    await page.close();
  });

  test('Karten-Editor lädt', async ({ browser }) => {
    const page = await qaPage(browser, 'dm');
    const w = watchPage(page);
    await page.goto(`/worlds/${IDS.forkWorldId}/map`);
    await waitVisible(page.getByText('Karten-Editor'));

    const shotPath = await shot(page, 'poi-map');
    reportHeuristics('poi-map', 'Karten-Editor', await heuristics(page), shotPath);
    summarizeWatch('poi-map', 'Karten-Editor', w, shotPath);
    note('poi-map', 'POI-Dialog', 'INFO',
      'POI-Klick ist PIXI-Canvas — "Aktionen"-Button nur per Code-Review geprüft.');
    await page.close();
  });

  test('Ortsansicht mobil (375px)', async ({ browser }) => {
    const page = await qaPage(browser, 'dm');
    await page.setViewportSize({ width: 375, height: 800 });
    const w = watchPage(page);
    await page.goto(`/worlds/${IDS.forkWorldId}/locations/${locationId}`);
    await waitVisible(page.getByText('Aktionen'));

    const shotPath = await shot(page, 'poi-location-mobile');
    reportHeuristics('poi-mobile', 'Ortsansicht mobil', await heuristics(page), shotPath);
    summarizeWatch('poi-mobile', 'Ortsansicht mobil', w, shotPath);
    await page.close();
  });
});
