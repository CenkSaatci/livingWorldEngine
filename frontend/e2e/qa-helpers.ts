import { Browser, Page, APIRequestContext, Locator, expect } from '@playwright/test';
import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'node:fs';

export const API = process.env.E2E_API_URL ?? 'http://localhost:8080';
export const WEB = process.env.E2E_BASE_URL ?? 'http://localhost:5173';
export const IDS: Record<string, string> = JSON.parse(
  readFileSync(process.env.QA_IDS ?? '/tmp/qa-ids.json', 'utf-8'),
);

export type Role = 'dm' | 'p1' | 'p2';
const ACCOUNTS: Record<Role, string> = {
  dm: 'playtest-meister@test.de',
  p1: 'playtest-spieler1@test.de',
  p2: 'playtest-spieler2@test.de',
};
const PASSWORD = process.env.QA_PASSWORD ?? 'Test123!';
const AUTH_DIR = 'e2e/.auth';

export interface QaIssue {
  spec: string;
  step: string;
  severity: 'FAIL' | 'WARN' | 'INFO';
  message: string;
  shot?: string;
}
export const qaIssues: QaIssue[] = [];

export function note(spec: string, step: string, severity: QaIssue['severity'], message: string, shot?: string) {
  qaIssues.push({ spec, step, severity, message, shot });
  // eslint-disable-next-line no-console
  console.log(`[QA-${severity}] ${spec} :: ${step} :: ${message}`);
}

/** Weiche Prüfung: sammelt FAIL, wirft nicht (Sweep läuft weiter). */
export function check(spec: string, step: string, cond: boolean, message: string, shot?: string): boolean {
  if (!cond) note(spec, step, 'FAIL', message, shot);
  return cond;
}

function statePath(role: Role) {
  return `${AUTH_DIR}/qa-${role}.json`;
}

export function tokenFor(role: Role): string {
  const state = JSON.parse(readFileSync(statePath(role), 'utf-8'));
  for (const origin of state.origins ?? []) {
    for (const item of origin.localStorage ?? []) {
      if (item.name === 'lwe:accessToken') return item.value as string;
    }
  }
  throw new Error(`accessToken fehlt in ${statePath(role)}`);
}

/** Loggt die 3 QA-Accounts per API ein und schreibt Storage-States (wiederverwendet wenn gültig). */
export async function ensureQaAuth(request: APIRequestContext) {
  mkdirSync(AUTH_DIR, { recursive: true });
  for (const role of Object.keys(ACCOUNTS) as Role[]) {
    let valid = false;
    if (existsSync(statePath(role))) {
      try {
        const check = await request.get(`${API}/api/v1/game-systems`, {
          headers: { Authorization: `Bearer ${tokenFor(role)}` },
        });
        valid = check.ok() || check.status() === 403;
      } catch {
        valid = false;
      }
    }
    if (valid) continue;
    const res = await request.post(`${API}/api/v1/auth/login`, {
      data: { email: ACCOUNTS[role], password: PASSWORD },
    });
    expect(res.ok(), `QA-Login fehlgeschlagen: ${role} ${res.status()}`).toBeTruthy();
    const body = await res.json();
    const { accessToken, refreshToken, ...user } = body;
    writeFileSync(
      statePath(role),
      JSON.stringify({
        cookies: [],
        origins: [
          {
            origin: WEB,
            localStorage: [
              { name: 'lwe:accessToken', value: accessToken },
              { name: 'lwe:refreshToken', value: refreshToken },
              { name: 'lwe:user', value: JSON.stringify(user) },
            ],
          },
        ],
      }),
    );
  }
}

export async function qaPage(browser: Browser, role: Role): Promise<Page> {
  const ctx = await browser.newContext({ storageState: statePath(role) });
  const page = await ctx.newPage();
  return page;
}

export interface PageWatch {
  consoleErrors: string[];
  pageErrors: string[];
  badRequests: string[];
}

export function watchPage(page: Page): PageWatch {
  const w: PageWatch = { consoleErrors: [], pageErrors: [], badRequests: [] };
  page.on('console', (m) => {
    if (m.type() === 'error') w.consoleErrors.push(m.text().slice(0, 300));
    if (m.type() === 'warning' && /i18n|missing|key/i.test(m.text())) {
      w.consoleErrors.push(`i18n-warning: ${m.text().slice(0, 200)}`);
    }
  });
  page.on('pageerror', (e) => w.pageErrors.push(String(e).slice(0, 300)));
  page.on('response', (r) => {
    if (r.status() >= 400) w.badRequests.push(`${r.status()} ${r.request().method()} ${r.url().replace(API, 'API')}`);
  });
  return w;
}

export function summarizeWatch(spec: string, step: string, w: PageWatch, shot?: string) {
  for (const m of w.consoleErrors) note(spec, step, 'WARN', `console: ${m}`, shot);
  for (const m of w.pageErrors) note(spec, step, 'FAIL', `pageerror: ${m}`, shot);
  for (const m of w.badRequests) note(spec, step, 'INFO', `http: ${m}`, shot);
  w.consoleErrors.length = 0;
  w.pageErrors.length = 0;
  w.badRequests.length = 0;
}

export async function shot(page: Page, name: string): Promise<string> {
  mkdirSync('e2e/qa-out/shots', { recursive: true });
  const path = `e2e/qa-out/shots/${name}.png`;
  await page.screenshot({ path, fullPage: false });
  return path;
}

export interface Heuristics {
  buttonsWithoutName: number;
  inputsWithoutLabel: number;
  imagesWithoutAlt: number;
  overflowX: boolean;
  undefinedTexts: number;
  uuidTexts: number;
}

export async function heuristics(page: Page): Promise<Heuristics> {
  return page.evaluate(() => {
    const btns = [...document.querySelectorAll('button')].filter(
      (b) => !(b.textContent ?? '').trim() && !b.getAttribute('aria-label') && !b.getAttribute('title'),
    ).length;
    const inputs = [...document.querySelectorAll('input,select,textarea')].filter((el) => {
      const id = (el as HTMLInputElement).id;
      return !el.getAttribute('aria-label') && !el.getAttribute('title')
        && !(id && document.querySelector(`label[for="${id}"]`));
    }).length;
    const imgs = [...document.querySelectorAll('img')].filter((i) => !i.getAttribute('alt')).length;
    const overflowX = document.documentElement.scrollWidth > window.innerWidth + 1;
    const bodyText = document.body.innerText ?? '';
    const undefinedTexts = (bodyText.match(/undefined|NaN|\[object Object\]/g) ?? []).length;
    const uuidTexts = (bodyText.match(/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/gi) ?? []).length;
    return { buttonsWithoutName: btns, inputsWithoutLabel: inputs, imagesWithoutAlt: imgs, overflowX, undefinedTexts, uuidTexts };
  });
}

export function reportHeuristics(spec: string, step: string, h: Heuristics, shot?: string) {
  if (h.buttonsWithoutName > 0) note(spec, step, 'WARN', `${h.buttonsWithoutName} Buttons ohne Namen`, shot);
  if (h.inputsWithoutLabel > 0) note(spec, step, 'WARN', `${h.inputsWithoutLabel} Inputs ohne Label`, shot);
  if (h.imagesWithoutAlt > 0) note(spec, step, 'WARN', `${h.imagesWithoutAlt} Bilder ohne Alt`, shot);
  if (h.overflowX) note(spec, step, 'WARN', 'horizontaler Overflow', shot);
  if (h.undefinedTexts > 0) note(spec, step, 'FAIL', `${h.undefinedTexts}× undefined/NaN/[object Object] im Text`, shot);
  if (h.uuidTexts > 0) note(spec, step, 'WARN', `${h.uuidTexts}× rohe UUID im sichtbaren Text`, shot);
}

export async function anyVisible(loc: Locator): Promise<boolean> {
  const n = await loc.count().catch(() => 0);
  for (let i = 0; i < n; i++) {
    if (await loc.nth(i).isVisible().catch(() => false)) return true;
  }
  return false;
}

export async function countVisible(loc: Locator): Promise<number> {
  const n = await loc.count().catch(() => 0);
  let c = 0;
  for (let i = 0; i < n; i++) {
    if (await loc.nth(i).isVisible().catch(() => false)) c++;
  }
  return c;
}

export function saveQaResults(suite: string) {
  mkdirSync('e2e/qa-out', { recursive: true });
  writeFileSync(
    `e2e/qa-out/${suite}-results.json`,
    JSON.stringify({ suite, at: new Date().toISOString(), issues: qaIssues }, null, 2),
  );
}
