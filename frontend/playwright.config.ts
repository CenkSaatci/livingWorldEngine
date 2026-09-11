import { defineConfig, devices } from '@playwright/test';

/**
 * E2E gegen die lokale Dev-Umgebung (pm2: lwe-frontend :5173, lwe-backend :8080).
 * Seriell (workers: 1): Auth-Rate-Limit (5/900s) und geteilte Dev-Daten.
 * Voraussetzung: `npx playwright install chromium` einmalig.
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  workers: 1,
  fullyParallel: false,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'setup', testMatch: /auth\.setup\.ts/ },
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'], storageState: 'e2e/.auth/user.json' },
      dependencies: ['setup'],
    },
  ],
});
