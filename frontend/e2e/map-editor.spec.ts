import { test, expect } from '@playwright/test';
import { IDS, API, ensureQaAuth, qaPage, tokenFor } from './qa-helpers';

/**
 * Karten-Editor: Upload → Anzeige (auch anonym, kein 401) → Region zeichnen/speichern.
 * Regression für den Befund "Kartenbild 401 / nicht sichtbar".
 */
const worldId = IDS.forkWorldId;
const TEST_REGION = 'QA-E2E-Region';

async function deleteTestRegions(request: import('@playwright/test').APIRequestContext) {
  const auth = { headers: { Authorization: `Bearer ${tokenFor('dm')}` } };
  const res = await request.get(`${API}/api/v1/worlds/${worldId}/regions`, auth);
  for (const r of (await res.json()) as { id: string; name: string }[]) {
    if (r.name === TEST_REGION) {
      await request.delete(`${API}/api/v1/worlds/${worldId}/regions/${r.id}`, auth);
    }
  }
}

test.describe.serial('Karten-Editor', () => {
  test.beforeAll(async ({ request }) => {
    await ensureQaAuth(request);
    await deleteTestRegions(request);
  });

  test('Karte hochladen und in der Spielansicht anzeigen', async ({ browser }) => {
    const page = await qaPage(browser, 'dm');
    const pageErrors: string[] = [];
    const uploadStatus: number[] = [];
    const image401: string[] = [];
    page.on('pageerror', (e) => pageErrors.push(String(e).slice(0, 200)));
    page.on('response', (r) => {
      if (r.url().includes('/map/upload')) uploadStatus.push(r.status());
      if (r.url().includes('/uploads/') && r.status() >= 400) image401.push(`${r.status()} ${r.url()}`);
    });

    await page.goto(`/worlds/${worldId}/map`);
    const uploadDone = page.waitForResponse(
      (r) => r.url().includes(`/worlds/${worldId}/map/upload`) && r.request().method() === 'POST',
    );
    await page.locator('input[type="file"]').setInputFiles('e2e/fixtures/map-test.png');
    expect((await uploadDone).status()).toBe(200);

    // Spielansicht: das Karten-<img> ist wirklich geladen (nicht kaputt/401).
    await page.goto(`/worlds/${worldId}`);
    const img = page.locator('img[alt="Map"]');
    await expect(img).toBeVisible({ timeout: 15_000 });
    await expect
      .poll(async () => img.evaluate((el) => (el as HTMLImageElement).naturalWidth), { timeout: 15_000 })
      .toBeGreaterThan(0);

    expect(uploadStatus).toContain(200);
    expect(image401, `4xx auf /uploads/: ${image401.join(', ')}`).toEqual([]);
    expect(pageErrors, `pageerrors: ${pageErrors.join(' | ')}`).toEqual([]);
    await page.close();
  });

  test('Region zeichnen und Polygon speichern', async ({ browser, request }) => {
    const page = await qaPage(browser, 'dm');
    await page.goto(`/worlds/${worldId}/map`);
    await expect(page.getByRole('heading', { name: /Karten-Editor|Map Editor/i })).toBeVisible({ timeout: 15_000 });

    // Region über den Editor anlegen (window.prompt).
    page.once('dialog', (d) => d.accept(TEST_REGION));
    await page.getByRole('button', { name: /Region erstellen|Create region/i }).click();

    const regionItem = page.locator('div.rounded.px-2.py-1', { hasText: TEST_REGION }).first();
    await expect(regionItem).toBeVisible({ timeout: 10_000 });

    // Zeichnen starten (Button in der Regionszeile, nicht der Modus-Umschalter).
    await regionItem.getByRole('button', { name: /^(Zeichnen|Neu zeichnen|Draw|Redraw)$/ }).click();

    const overlay = page.locator('div.cursor-crosshair');
    await expect(overlay).toBeVisible();
    const box = await overlay.boundingBox();
    expect(box).not.toBeNull();
    for (const [fx, fy] of [[0.2, 0.2], [0.6, 0.2], [0.6, 0.6]] as const) {
      await page.mouse.click(box!.x + box!.width * fx, box!.y + box!.height * fy);
    }
    await page.getByRole('button', { name: /Polygon speichern|Save polygon/i }).click();

    // API-Gegenprobe: Polygon ist persistiert.
    const auth = { headers: { Authorization: `Bearer ${tokenFor('dm')}` } };
    await expect
      .poll(async () => {
        const res = await request.get(`${API}/api/v1/worlds/${worldId}/regions`, auth);
        const list = (await res.json()) as { name: string; polygonPoints?: string }[];
        return list.find((r) => r.name === TEST_REGION)?.polygonPoints ?? null;
      }, { timeout: 10_000 })
      .not.toBeNull();

    const res = await request.get(`${API}/api/v1/worlds/${worldId}/regions`, auth);
    const created = ((await res.json()) as { name: string; polygonPoints?: string }[])
      .find((r) => r.name === TEST_REGION)!;
    expect(JSON.parse(created.polygonPoints!).length).toBe(3);

    // F-1: Das gezeichnete Polygon ist auch in der Spielansicht sichtbar.
    await page.goto(`/worlds/${worldId}`);
    await expect(page.locator('img[alt="Map"]')).toBeVisible({ timeout: 15_000 });
    await expect(page.locator('svg polygon').first()).toBeVisible({ timeout: 15_000 });

    await page.close();
  });

  test.afterAll(async ({ request }) => {
    await deleteTestRegions(request);
  });
});
