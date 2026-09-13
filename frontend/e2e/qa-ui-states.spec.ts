import { test, expect, Browser } from '@playwright/test';
import {
  IDS, qaIssues, note, check, ensureQaAuth, qaPage,
  watchPage, summarizeWatch, shot, reportHeuristics, heuristics, saveQaResults,
} from './qa-helpers';

const SPEC = 'qa-ui-states';

test.describe.configure({ mode: 'serial' });

test.beforeAll(async ({ request, browser }) => {
  await ensureQaAuth(request);
  void browser;
});

test.afterAll(() => {
  saveQaResults('ui-states');
});

const MATRIX: { key: string; path: string; marker?: RegExp | string }[] = [
  { key: 'dashboard', path: '/dashboard' },
  { key: 'systems', path: '/game-systems' },
  { key: 'campaign', path: `/campaigns/${IDS.campaignId}` },
  { key: 'world', path: `/worlds/${IDS.forkWorldId}` },
  { key: 'entities', path: `/worlds/${IDS.forkWorldId}/entities` },
  { key: 'sheet', path: `/characters/${IDS.miraId}` },
  { key: 'npc', path: `/worlds/${IDS.forkWorldId}/npcs/${IDS.alrikId}` },
  { key: 'adventure', path: `/worlds/${IDS.forkWorldId}/adventures/${IDS.adventureId}` },
  { key: 'quest', path: `/worlds/${IDS.forkWorldId}/quests/${IDS.questId}` },
  { key: 'settings', path: '/settings' },
];

test('UI-Matrix: Seiten laden ohne Fehler, Screenshots, Heuristiken', async ({ browser }) => {
  const page = await qaPage(browser, 'dm');
  const w = watchPage(page);
  for (const m of MATRIX) {
    await page.goto(m.path);
    await page.waitForTimeout(1500);
    const s = await shot(page, `ui-${m.key}-desktop`);
    const h = await heuristics(page);
    reportHeuristics(SPEC, `UI:${m.key}`, h, s);
    check(SPEC, `UI:${m.key}`, h.undefinedTexts === 0, 'Seite rendert ohne undefined/NaN');
    summarizeWatch(SPEC, `UI:${m.key}`, w, s);
    // mobil
    await page.setViewportSize({ width: 375, height: 800 });
    await page.waitForTimeout(800);
    const sm = await shot(page, `ui-${m.key}-mobile`);
    const hm = await heuristics(page);
    if (hm.overflowX) note(SPEC, `UI:${m.key}`, 'WARN', 'horizontaler Overflow bei 375px', sm);
    await page.setViewportSize({ width: 1280, height: 800 });
  }
  await page.context().close();
});

test('Admin-Seite als USER: Zugriff verweigert', async ({ browser }) => {
  const page = await qaPage(browser, 'p1');
  const w = watchPage(page);
  await page.goto('/admin');
  await page.waitForTimeout(1200);
  const url = page.url();
  const body = (await page.content()).slice(0, 2000);
  const denied = /login|dashboard|403|verweigert|denied|keine Berechtigung/i.test(url + body);
  check(SPEC, 'UI:admin', denied, `Admin-Seite als USER erreichbar ohne Deny? URL=${url}`);
  await shot(page, 'ui-admin-denied');
  summarizeWatch(SPEC, 'UI:admin', w);
  await page.context().close();
});

test('Registrierung: neuer Account + Empty States', async ({ browser }) => {
  const ctx = await browser.newContext();
  const page = await ctx.newPage();
  const w = watchPage(page);
  const stamp = Date.now();
  const email = `qa-sweep-${stamp}@test.de`;
  await page.goto('/register');
  await page.waitForTimeout(1000);
  await shot(page, 'ui-register');
  const emailInput = page.locator('input[type="email"], input[name="email"]').first();
  const hasForm = await emailInput.isVisible({ timeout: 10_000 }).catch(() => false);
  check(SPEC, 'UI:register', hasForm, 'Register-Formular fehlt');
  if (hasForm) {
    await emailInput.fill(email);
    const userInput = page.locator('input[name="username"], input[placeholder*="Name" i]').first();
    if (await userInput.isVisible().catch(() => false)) await userInput.fill(`qa-sweep-${stamp}`);
    const pwInputs = page.locator('input[type="password"]');
    if ((await pwInputs.count()) >= 1) await pwInputs.first().fill('Test123!');
    if ((await pwInputs.count()) >= 2) await pwInputs.nth(1).fill('Test123!');
    const [resp] = await Promise.all([
      page.waitForResponse((r) => r.url().includes('/auth/register'), { timeout: 15_000 }).catch(() => null),
      page.getByRole('button', { name: /Registrieren|Register|Konto erstellen/i }).click(),
    ]);
    check(SPEC, 'UI:register', !!resp && resp.ok(), 'Registrierung: keine erfolgreiche Response');
    await page.waitForTimeout(1500);
    await shot(page, 'ui-fresh-dashboard');
    const h = await heuristics(page);
    reportHeuristics(SPEC, 'UI:fresh', h);
    note(SPEC, 'UI:fresh', 'INFO', `Frisch-Account ${email} angelegt (bleibt als Testdaten bestehen)`);
  }
  summarizeWatch(SPEC, 'UI:register', w);
  await ctx.close();
});

test('Login-Seite: Darstellung + Fehlermeldung', async ({ browser }) => {
  const ctx = await browser.newContext();
  const page = await ctx.newPage();
  const w = watchPage(page);
  await page.goto('/login');
  await page.waitForTimeout(1000);
  await shot(page, 'ui-login');
  await page.locator('input[type="email"], input[name="email"]').first().fill('gibts-nicht@test.de');
  await page.locator('input[type="password"]').first().fill('falsch123');
  await page.getByRole('button', { name: /Anmelden|Login|Einloggen/i }).click();
  await page.waitForTimeout(1500);
  await shot(page, 'ui-login-failed');
  const txt = await page.content();
  const hasMsg = /falsch|ungültig|invalid|fehlgeschlagen|failed|error/i.test(txt);
  check(SPEC, 'UI:login', hasMsg, 'Kein sichtbarer Fehlertext bei falschem Login');
  reportHeuristics(SPEC, 'UI:login', await heuristics(page));
  summarizeWatch(SPEC, 'UI:login', w);
  await ctx.close();
});
