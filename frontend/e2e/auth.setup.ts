import { test as setup, expect } from '@playwright/test';

const API = process.env.E2E_API_URL ?? 'http://localhost:8080';

/**
 * Login einmal pro Lauf (Rate-Limit 5/900s) und Storage-State speichern.
 * Env-Overrides: E2E_EMAIL / E2E_PASSWORD.
 */
setup('login and persist storage state', async ({ request, page }) => {
  const res = await request.post(`${API}/api/v1/auth/login`, {
    data: {
      email: process.env.E2E_EMAIL ?? 'devbe@test.de',
      password: process.env.E2E_PASSWORD ?? 'Test123!',
    },
  });
  expect(res.ok(), `Login fehlgeschlagen: ${res.status()}`).toBeTruthy();
  const body = await res.json();

  await page.goto('/login');
  await page.evaluate(([{ accessToken, refreshToken, ...user }]) => {
    localStorage.setItem('lwe:accessToken', accessToken);
    localStorage.setItem('lwe:refreshToken', refreshToken);
    localStorage.setItem('lwe:user', JSON.stringify(user));
  }, [body]);
  await page.context().storageState({ path: 'e2e/.auth/user.json' });
});
