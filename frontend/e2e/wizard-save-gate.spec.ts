import { test, expect } from '@playwright/test';

/**
 * P31-T03: Save-Gate im SystemWizard — ohne Attribute blockiert der Save mit Toast
 * (kein API-Call, nichts wird gespeichert).
 */
test.describe('Wizard-Save-Gate (P31-T03)', () => {
  const name = `E2E Gate ${Date.now()}`;

  test('blockiert Speichern ohne Attribute mit Prüfbericht-Toast', async ({ page }) => {
    await page.goto('/game-systems');
    await page.getByRole('button', { name: 'New System' }).click();

    // Step 0 (System) -> Step 1 (Basic): Namen setzen, damit nur das Attribut-Gate greift
    await page.getByRole('button', { name: 'Weiter', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Basic Information' })).toBeVisible();
    await page.getByRole('textbox').first().fill(name);

    const uebersicht = page.getByRole('heading', { name: 'Übersicht & Speichern' });
    for (let i = 0; i < 14 && !(await uebersicht.isVisible().catch(() => false)); i++) {
      await page.getByRole('button', { name: 'Weiter', exact: true }).click();
    }
    await expect(uebersicht).toBeVisible();

    await page.getByRole('button', { name: 'System Speichern' }).click();

    // Kein Save: Modal bleibt offen, Toast mit dem Attribut-Hinweis erscheint
    await expect(page.getByText('Mindestens ein Attribut anlegen vor dem Speichern')).toBeVisible({ timeout: 5_000 });
    await expect(uebersicht).toBeVisible();

    // Kein neues System angelegt: frische Liste zeigt keinen Edit-Button
    await page.goto('/game-systems');
    await expect(page.getByRole('button', { name: `Edit ${name}` })).toHaveCount(0);
  });
});
