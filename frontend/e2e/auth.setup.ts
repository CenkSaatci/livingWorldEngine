import { test as setup, expect } from '@playwright/test';
import { readFileSync } from 'node:fs';

const API = process.env.E2E_API_URL ?? 'http://localhost:8080';

/** Token aus vorhandenem Storage-State (falls Login rate-limited ist). */
function tokenFromState(): string | null {
  try {
    const state = JSON.parse(readFileSync('e2e/.auth/user.json', 'utf-8'));
    for (const origin of state.origins ?? []) {
      for (const item of origin.localStorage ?? []) {
        if (item.name === 'lwe:accessToken') return item.value as string;
      }
    }
  } catch {
    /* kein State vorhanden */
  }
  return null;
}

/**
 * Login einmal pro Lauf (Rate-Limit 5/900s) und Storage-State speichern.
 * Env-Overrides: E2E_EMAIL / E2E_PASSWORD.
 * Bei 429 wird ein noch gültiger vorhandener State weiterverwendet.
 */
setup('login and persist storage state', async ({ request, page }) => {
  const res = await request.post(`${API}/api/v1/auth/login`, {
    data: {
      email: process.env.E2E_EMAIL ?? 'devbe@test.de',
      password: process.env.E2E_PASSWORD ?? 'Test123!',
    },
  });

  if (!res.ok()) {
    const existing = tokenFromState();
    if (res.status() === 429 && existing) {
      const check = await request.get(`${API}/api/v1/game-systems`, {
        headers: { Authorization: `Bearer ${existing}` },
      });
      if (check.ok()) {
        // bestehender State ist noch gültig — Rate-Limit umgehen
        return;
      }
    }
    expect(res.ok(), `Login fehlgeschlagen: ${res.status()}`).toBeTruthy();
  }

  const body = await res.json();
  await page.goto('/login');
  await page.evaluate(([{ accessToken, refreshToken, ...user }]) => {
    localStorage.setItem('lwe:accessToken', accessToken);
    localStorage.setItem('lwe:refreshToken', refreshToken);
    localStorage.setItem('lwe:user', JSON.stringify(user));
  }, [body]);
  await page.context().storageState({ path: 'e2e/.auth/user.json' });
});
