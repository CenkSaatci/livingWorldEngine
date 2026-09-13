import { test, expect, Browser } from '@playwright/test';
import {
  API, IDS, Role, qaIssues, note, check, ensureQaAuth, qaPage, tokenFor,
  watchPage, summarizeWatch, shot, reportHeuristics, heuristics, saveQaResults,
  anyVisible, countVisible,
} from './qa-helpers';

const SPEC = 'qa-golden';
let uiPcId = '';
let combatSessionId = '';

test.describe.configure({ mode: 'serial' });

test.beforeAll(async ({ request, browser }) => {
  await ensureQaAuth(request);
  // Platzhalter: Browser-Fixture wird pro Test via qaPage erzeugt.
  void browser;
});

test('GP-15 Spieler-Löschversuch an fremder Entity wird abgewiesen', async ({ browser, request }) => {
  const pages = await openPages(browser, ['p1']);
  const page = pages.p1;
  const w = watchPage(page);
  const mk = await request.post(`${API}/api/v1/worlds/${IDS.forkWorldId}/entities`, {
    headers: { Authorization: `Bearer ${tokenFor('dm')}` },
    data: { entityType: 'NPC', name: 'QA-Wegwerf', attributesJson: '{}' },
  });
  check(SPEC, 'GP-15', mk.ok(), 'Scratch-NPC konnte nicht angelegt werden');
  const sid = mk.ok() ? (await mk.json()).id : '';
  await page.goto(`/worlds/${IDS.forkWorldId}/entities`);
  const row = page.locator('tr', { hasText: 'QA-Wegwerf' });
  const hasRow = await anyVisible(row);
  check(SPEC, 'GP-15', hasRow, 'Scratch-NPC nicht in P1-Liste');
  const trash = row.getByRole('button');
  const trashVisible = await anyVisible(trash);
  note(SPEC, 'GP-15', 'INFO', trashVisible
    ? 'Löschen-Button für Spieler sichtbar (API muss 403 liefern)'
    : 'Kein Löschen-Button für Spieler (gut, falls beabsichtigt)');
  if (hasRow && trashVisible && sid) {
    page.on('dialog', (d) => d.accept().catch(() => {}));
    const [delResp] = await Promise.all([
      page.waitForResponse((r) => r.request().method() === 'DELETE' && r.url().includes('/entities/'), { timeout: 15_000 }).catch(() => null),
      trash.first().click(),
    ]);
    check(SPEC, 'GP-15', !!delResp && delResp.status() === 403, `Löschversuch: Status ${delResp?.status()} (erwartet 403)`);
    await page.waitForTimeout(1000);
    const still = await request.get(`${API}/api/v1/worlds/${IDS.forkWorldId}/entities/${sid}`, {
      headers: { Authorization: `Bearer ${tokenFor('dm')}` },
    });
    check(SPEC, 'GP-15', still.ok(), 'Entity nach abgelehntem Löschversuch weg?!');
    await shot(page, 'gp15-delete-denied');
  }
  if (sid) {
    await request.delete(`${API}/api/v1/worlds/${IDS.forkWorldId}/entities/${sid}`, {
      headers: { Authorization: `Bearer ${tokenFor('dm')}` },
    }).catch(() => {});
  }
  summarizeWatch(SPEC, 'GP-15', w);
  await closePages(pages);
});

test.afterAll(async ({ request }) => {
  if (combatSessionId) {
    await request.post(`${API}/api/v1/combat/${combatSessionId}/end`, {
      headers: { Authorization: `Bearer ${tokenFor('dm')}` },
    }).catch(() => {});
  }
  if (uiPcId) {
    await request.delete(`${API}/api/v1/worlds/${IDS.forkWorldId}/entities/${uiPcId}`, {
      headers: { Authorization: `Bearer ${tokenFor('p1')}` },
    }).catch(() => {});
  }
  saveQaResults('golden');
});

async function openPages(browser: Browser, roles: Role[]) {
  const pages: Record<string, Awaited<ReturnType<typeof qaPage>>> = {};
  for (const r of roles) pages[r] = await qaPage(browser, r);
  return pages;
}
async function closePages(pages: Record<string, Awaited<ReturnType<typeof qaPage>>>) {
  for (const p of Object.values(pages)) await p.context().close();
}

test('GP-04 DM sieht Kampagne, Welt und Entities', async ({ browser }) => {
  const pages = await openPages(browser, ['dm']);
  const page = pages.dm;
  const w = watchPage(page);
  await page.goto(`/campaigns/${IDS.campaignId}`);
  const okH = await anyVisible(page.getByRole('heading', { name: 'QA-Runde' }));
  check(SPEC, 'GP-04', okH, 'Kampagnen-Heading QA-Runde fehlt');
  await shot(page, 'gp04-campaign');
  await page.getByRole('button', { name: 'In Welt starten' }).click();
  await expect(page).toHaveURL(new RegExp(`/worlds/${IDS.forkWorldId}$`), { timeout: 10_000 });
  await page.goto(`/worlds/${IDS.forkWorldId}/entities`);
  const okM = await anyVisible(page.getByText('QA-Mira', { exact: true }));
  check(SPEC, 'GP-04', okM, 'QA-Mira nicht in Entity-Liste');
  const okA = await anyVisible(page.getByText('QA-Alrik', { exact: true }));
  check(SPEC, 'GP-04', okA, 'QA-Alrik nicht in Entity-Liste');
  await shot(page, 'gp04-entities');
  reportHeuristics(SPEC, 'GP-04', await heuristics(page));
  summarizeWatch(SPEC, 'GP-04', w);
  await closePages(pages);
});

test('GP-05/06 Wizard: Save-Gate + PC-Erstellung durch Spieler', async ({ browser }) => {
  const pages = await openPages(browser, ['p1']);
  const page = pages.p1;
  const w = watchPage(page);
  await page.goto(`/campaigns/${IDS.campaignId}`);
  const startBtn = page.getByRole('button', { name: 'In Welt starten' });
  if (await anyVisible(startBtn)) {
    await startBtn.first().click();
    await expect(page).toHaveURL(new RegExp(`/worlds/${IDS.forkWorldId}$`), { timeout: 10_000 });
  }
  await page.goto(`/worlds/${IDS.forkWorldId}/entities`);
  const wizardBtn = page.getByRole('button', { name: 'Charakter-Wizard' });
  const hasWizard = await anyVisible(wizardBtn);
  check(SPEC, 'GP-05', hasWizard, 'Charakter-Wizard-Button fehlt für Spieler (aktive Kampagne gesetzt?)');
  if (!hasWizard) { await shot(page, 'gp05-no-wizard'); summarizeWatch(SPEC, 'GP-05', w); await closePages(pages); return; }
  const wb = wizardBtn.filter({ hasNot: page.locator('[hidden]') });
  if (await wb.count() > 1) {
    const vis = [];
    for (let i = 0; i < await wb.count(); i++) if (await wb.nth(i).isVisible().catch(() => false)) vis.push(i);
    await wb.nth(vis[0] ?? 0).click();
  } else {
    await wizardBtn.first().click();
  }
  await expect(page.getByRole('heading', { name: 'Charakter erstellen' })).toBeVisible({ timeout: 10_000 });
  await shot(page, 'gp05-wizard-start');
  // Weiter klicken bis Speichern erscheint (max 10 Schritte), Paket-Selects ggf. wählen
  let saved = false;
  for (let i = 0; i < 10 && !saved; i++) {
    const combos = page.getByRole('combobox');
    const n = await combos.count();
    for (let c = 0; c < n; c++) {
      const cb = combos.nth(c);
      if (!(await cb.isVisible().catch(() => false))) continue;
      const opts = cb.locator('option');
      const on = await opts.count();
      for (let o = 1; o < on; o++) {
        const v = await opts.nth(o).getAttribute('value');
        if (v) { await cb.selectOption(v).catch(() => {}); break; }
      }
    }
    if (await page.getByRole('button', { name: 'Speichern', exact: true }).isVisible().catch(() => false)) {
      const nameInput = page.getByLabel(/Name/);
      if (await nameInput.isVisible().catch(() => false)) await nameInput.fill('QA-Mira-UI');
      await shot(page, 'gp05-wizard-overview');
      const [resp] = await Promise.all([
        page.waitForResponse((r) => r.url().includes('/entities') && r.request().method() === 'POST', { timeout: 15_000 }).catch(() => null),
        page.getByRole('button', { name: 'Speichern', exact: true }).click(),
      ]);
      if (resp && resp.ok()) {
        const body = await resp.json().catch(() => null);
        uiPcId = body?.id ?? '';
        saved = true;
      } else {
        note(SPEC, 'GP-05', 'WARN', 'Speichern: keine erfolgreiche Entity-POST-Response (Save-Gate? Toast prüfen)');
        await shot(page, 'gp05-save-blocked');
        break;
      }
    } else {
      const weiter = page.getByRole('button', { name: 'Weiter', exact: true });
      if (await weiter.isVisible().catch(() => false)) await weiter.click();
      else break;
    }
  }
  check(SPEC, 'GP-05/06', saved, 'Wizard-Flow konnte nicht bis Speichern durchlaufen werden');
  if (saved) {
    await expect(page).toHaveURL(/\/characters\/[0-9a-f-]+$/, { timeout: 10_000 });
    await shot(page, 'gp06-pc-created');
  }
  reportHeuristics(SPEC, 'GP-05', await heuristics(page));
  summarizeWatch(SPEC, 'GP-05', w);
  await closePages(pages);
});

test('GP-09/CHR-07 P1 würfelt Probe vom Sheet', async ({ browser, request }) => {
  const pages = await openPages(browser, ['p1']);
  const page = pages.p1;
  const w = watchPage(page);
  await page.goto(`/characters/${IDS.miraId}`);
  const okH = await anyVisible(page.getByRole('heading', { name: 'QA-Mira' }));
  check(SPEC, 'GP-09', okH, 'Sheet-Heading QA-Mira fehlt');
  const noSkills = await anyVisible(page.getByText('Keine Fertigkeiten', { exact: true }));
  check(SPEC, 'GP-09', !noSkills, 'Sheet zeigt „Keine Fertigkeiten" trotz skillsJson (Kampagnen-Kontext?)');
  const row = page.getByText('Überreden', { exact: true }).locator('xpath=ancestor::div[contains(@class,"justify-between")][1]');
  const hasRow = await anyVisible(row);
  check(SPEC, 'CHR-07', hasRow, 'Skill-Zeile Überreden fehlt');
  if (hasRow) {
    const [resp] = await Promise.all([
      page.waitForResponse((r) => r.url().includes('/rolls/probe') && r.request().method() === 'POST', { timeout: 15_000 }).catch(() => null),
      row.locator('button[title="Probe würfeln"]').click(),
    ]);
    check(SPEC, 'CHR-07', !!resp && resp.ok(), 'Keine erfolgreiche /rolls/probe-Response nach Würfel-Klick');
    await page.waitForTimeout(800);
    const marker = row.locator('button.text-success, button.text-danger');
    check(SPEC, 'CHR-07', (await marker.count()) > 0, 'Kein Ergebnis-Marker (Erfolg/Misserfolg-Farbe) in Skill-Zeile');
    await shot(page, 'gp09-probe-result');
  }
  reportHeuristics(SPEC, 'GP-09', await heuristics(page));
  summarizeWatch(SPEC, 'GP-09', w);
  // Fate-Punkte per API lesen (Vorher/Nachher passiert in CHR-Fate-Test)
  const ent = await request.get(`${API}/api/v1/worlds/${IDS.forkWorldId}/entities/${IDS.miraId}`, {
    headers: { Authorization: `Bearer ${tokenFor('p1')}` },
  });
  check(SPEC, 'GP-09', ent.ok(), 'Entity-GET für Fate-Check fehlgeschlagen');
  await closePages(pages);
});

test('CHR-09 Fate +★ gibt Punkt aus und addiert Bonus', async ({ browser, request }) => {
  const pages = await openPages(browser, ['p1']);
  const page = pages.p1;
  const w = watchPage(page);
  const fateOf = async () => {
    const r = await request.get(`${API}/api/v1/worlds/${IDS.forkWorldId}/entities/${IDS.miraId}`, {
      headers: { Authorization: `Bearer ${tokenFor('p1')}` },
    });
    const j = await r.json();
    return JSON.parse(j.metadataJson ?? '{}').fate_points as number | undefined;
  };
  const before = await fateOf();
  await page.goto(`/characters/${IDS.miraId}`);
  const row = page.getByText('Überreden', { exact: true }).locator('xpath=ancestor::div[contains(@class,"justify-between")][1]');
  await row.isVisible({ timeout: 10_000 });
  const toggle = row.locator('button[title="Schicksalspunkt für +Bonus auf diese Probe ausgeben"]');
  const hasToggle = await toggle.isVisible({ timeout: 5_000 }).catch(() => false);
  check(SPEC, 'CHR-09', hasToggle, '+★-Toggle fehlt (fateAvailable?/bereits Ergebnis?)');
  if (hasToggle) {
    await toggle.click();
    const [resp] = await Promise.all([
      page.waitForResponse((r) => r.url().includes('/rolls/probe') && r.request().method() === 'POST', { timeout: 15_000 }).catch(() => null),
      row.locator('button[title="Probe würfeln"]').click(),
    ]);
    check(SPEC, 'CHR-09', !!resp && resp.ok(), 'Fate-Probe: keine erfolgreiche Response');
    const after = await fateOf();
    check(SPEC, 'CHR-09', before !== undefined && after === before - 1, `Fate-Punkte: vorher=${before} nachher=${after} (erwartet -1)`);
    await shot(page, 'chr09-fate-used');
  }
  summarizeWatch(SPEC, 'CHR-09', w);
  await closePages(pages);
});

test('GP-08 Chat zu dritt + Historie', async ({ browser }) => {
  const pages = await openPages(browser, ['dm', 'p1', 'p2']);
  const w1 = watchPage(pages.p1);
  const w2 = watchPage(pages.p2);
  const msg = `Hallo QA ${Date.now()}`;
  for (const r of ['dm', 'p1', 'p2'] as const) {
    await pages[r].goto(`/worlds/${IDS.forkWorldId}`);
    await pages[r].locator('[data-chat-input]').isVisible({ timeout: 15_000 }).catch(() => {});
  }
  const chatVisible = await pages.p1.locator('[data-chat-input]').isVisible().catch(() => false);
  check(SPEC, 'GP-08', chatVisible, 'Chat-Input ([data-chat-input]) nicht sichtbar');
  if (!chatVisible) {
    for (const r of ['dm', 'p1', 'p2'] as const) await shot(pages[r], `gp08-no-chat-${r}`);
    summarizeWatch(SPEC, 'GP-08', w1); summarizeWatch(SPEC, 'GP-08', w2);
    await closePages(pages); return;
  }
  await pages.p1.locator('[data-chat-input]').fill(msg);
  await pages.p1.locator('[data-chat-input]').press('Enter');
  await pages.p1.waitForTimeout(2500);
  const cP1 = await countVisible(pages.p1.getByText(msg));
  check(SPEC, 'GP-08', cP1 === 1, `P1 sieht eigene Nachricht ${cP1}× (erwartet genau 1×, Dedupe)`);
  const seenP2 = await anyVisible(pages.p2.getByText(msg));
  check(SPEC, 'GP-08', seenP2, 'P2 sieht P1-Nachricht nicht (WS)');
  if (seenP2) {
    const cP2 = await countVisible(pages.p2.getByText(msg));
    check(SPEC, 'GP-08', cP2 === 1, `P2 sieht Nachricht ${cP2}× (erwartet 1×)`);
  }
  const seenDm = await anyVisible(pages.dm.getByText(msg));
  check(SPEC, 'GP-08', seenDm, 'DM sieht P1-Nachricht nicht (WS)');
  await pages.p2.reload();
  await pages.p2.waitForTimeout(2000);
  const persisted = await anyVisible(pages.p2.getByText(msg));
  check(SPEC, 'GP-08', persisted, 'Nachricht nach Reload weg (Historie)');
  await shot(pages.p1, 'gp08-chat');
  summarizeWatch(SPEC, 'GP-08', w1); summarizeWatch(SPEC, 'GP-08', w2);
  await closePages(pages);
});

test('GP-10 Handel P1↔P2 komplett in UI', async ({ browser, request }) => {
  const pages = await openPages(browser, ['p1', 'p2']);
  const w = watchPage(pages.p1);
  await pages.p1.goto(`/characters/${IDS.miraId}`);
  const tradeBtn = pages.p1.getByRole('button', { name: /Handeln/ });
  const hasTrade = await tradeBtn.isVisible({ timeout: 10_000 }).catch(() => false);
  check(SPEC, 'GP-10', hasTrade, 'Handeln-Button auf Sheet fehlt');
  if (!hasTrade) { await shot(pages.p1, 'gp10-no-trade'); summarizeWatch(SPEC, 'GP-10', w); await closePages(pages); return; }
  await tradeBtn.click();
  const dialog = pages.p1.getByRole('dialog');
  check(SPEC, 'GP-10', await dialog.isVisible({ timeout: 10_000 }).catch(() => false), 'TradeModal öffnet nicht');
  await shot(pages.p1, 'gp10-trade-open');
  const partner = dialog.getByRole('combobox').first();
  await partner.selectOption(IDS.torbenId).catch(() => {});
  const offerBtn = dialog.getByRole('button', { name: 'Du gibst QA-Schwert +' });
  const hasOffer = await offerBtn.isVisible({ timeout: 10_000 }).catch(() => false);
  check(SPEC, 'GP-10', hasOffer, 'Angebot-Button für QA-Schwert fehlt');
  if (hasOffer) {
    await offerBtn.click();
    const [resp] = await Promise.all([
      pages.p1.waitForResponse((r) => r.url().includes('/api/v1/trades') && r.request().method() === 'POST', { timeout: 15_000 }).catch(() => null),
      dialog.getByRole('button', { name: 'Angebot senden' }).click(),
    ]);
    check(SPEC, 'GP-10', !!resp && resp.ok(), 'Angebot senden: keine erfolgreiche POST /trades');
    await shot(pages.p1, 'gp10-offer-sent');
    // P2 nimmt an
    await pages.p2.goto(`/characters/${IDS.torbenId}`);
    const t2btn = pages.p2.getByRole('button', { name: /Handeln/ });
    const t2vis = await anyVisible(t2btn);
    check(SPEC, 'GP-10', t2vis, 'Handeln-Button auf Torben-Sheet fehlt');
    if (t2vis) {
      await t2btn.first().click();
      const dlg2 = pages.p2.getByRole('dialog');
      check(SPEC, 'GP-10', await anyVisible(dlg2), 'TradeModal öffnet nicht (P2)');
      await shot(pages.p2, 'gp10-p2-modal');
      const accept = dlg2.getByRole('button', { name: 'Annehmen' });
      const hasAccept = await accept.first().isVisible({ timeout: 15_000 }).catch(() => false);
      check(SPEC, 'GP-10', hasAccept, 'P2 sieht kein „Annehmen" für eingehenden Trade');
      if (hasAccept) {
        const [accResp] = await Promise.all([
          pages.p2.waitForResponse((r) => r.url().includes('/accept') && r.request().method() === 'POST', { timeout: 15_000 }).catch(() => null),
          accept.first().click(),
        ]);
        check(SPEC, 'GP-10', !!accResp && accResp.ok(), 'Annehmen: keine erfolgreiche Response');
        await shot(pages.p2, 'gp10-accepted');
      }
    }
    // Inventare per API verifizieren
    const inv = async (eid: string, tok: string) => {
      const r = await request.get(`${API}/api/v1/entities/${eid}/inventory`, {
        headers: { Authorization: `Bearer ${tok}` },
      });
      return r.ok() ? await r.json() : null;
    };
    const invM = await inv(IDS.miraId, tokenFor('p1'));
    const invT = await inv(IDS.torbenId, tokenFor('p2'));
    const swordAt = (list: unknown) => JSON.stringify(list ?? '').includes(IDS.swordId);
    check(SPEC, 'GP-10', !swordAt(invM) && swordAt(invT), 'Schwert nach Trade nicht bei Torben (Inventare prüfen)');
  }
  summarizeWatch(SPEC, 'GP-10', w);
  await closePages(pages);
});

test('GP-11 Kampf zu dritt in UI (Angriff + Manöver + Ende)', async ({ browser }) => {
  const pages = await openPages(browser, ['dm', 'p1']);
  const w = watchPage(pages.dm);
  await pages.dm.goto(`/campaigns/${IDS.campaignId}`);
  await pages.dm.getByRole('button', { name: 'In Welt starten' }).click();
  await expect(pages.dm).toHaveURL(new RegExp(`/worlds/${IDS.forkWorldId}$`), { timeout: 10_000 });
  await pages.dm.getByRole('button', { name: 'Kampf' }).click();
  await pages.dm.getByRole('checkbox', { name: /QA-Mira/ }).first().check();
  await pages.dm.getByRole('checkbox', { name: /QA-Räuber/ }).first().check();
  const [startResp] = await Promise.all([
    pages.dm.waitForResponse((r) => r.url().includes('/combat') && r.request().method() === 'POST', { timeout: 15_000 }).catch(() => null),
    pages.dm.getByRole('button', { name: 'Kampf starten', exact: true }).click(),
  ]);
  check(SPEC, 'GP-11', !!startResp && startResp.ok(), 'Kampf starten: keine erfolgreiche Response');
  if (startResp && startResp.ok()) {
    const body = await startResp.json().catch(() => null);
    combatSessionId = body?.session?.id ?? body?.id ?? '';
  }
  await pages.dm.waitForTimeout(1500);
  await shot(pages.dm, 'gp11-combat-open');
  // Ziel wählen + Angriff (Wuchtschlag bevorzugt, sonst erster aktiver Angriffs-Button)
  const targetSection = pages.dm.getByText('Ziel', { exact: true }).locator('..');
  if (await targetSection.isVisible({ timeout: 10_000 }).catch(() => false)) {
    await targetSection.getByRole('button').first().click();
  }
  const wuchtschlag = pages.dm.getByRole('button', { name: /Wuchtschlag/ });
  if (await anyVisible(wuchtschlag)) {
    const dis = await wuchtschlag.first().isDisabled().catch(() => null);
    if (dis) note(SPEC, 'GP-11', 'FAIL', 'Wuchtschlag permanent deaktiviert (apCost 2 > AP-Max 1 im QA-System)');
  }
  const attack = pages.dm.getByRole('button', { name: 'Angreifen', exact: true });
  let acted = false;
  if (await anyVisible(attack)) {
    const [aResp] = await Promise.all([
      pages.dm.waitForResponse((r) => r.url().includes('/combat/') && r.url().includes('/action') && r.request().method() === 'POST', { timeout: 15_000 }).catch(() => null),
      attack.first().click(),
    ]);
    check(SPEC, 'GP-11', !!aResp && aResp.ok(), 'Angreifen: keine erfolgreiche Response');
    if (aResp && aResp.ok()) {
      const b = await aResp.json().catch(() => null);
      note(SPEC, 'GP-11', 'INFO', `Angriff: actionType=${b?.actionType} damage=${b?.totalDamage}`);
    }
    acted = !!aResp?.ok();
  }
  if (!acted) note(SPEC, 'GP-11', 'WARN', 'Kein Angriffs-Button ausführbar (Turn?/Selektoren prüfen)');
  await shot(pages.dm, 'gp11-after-action');
  const endBtn = pages.dm.getByRole('button', { name: 'Beenden', exact: true });
  if (await anyVisible(endBtn)) {
    await endBtn.first().click();
    await pages.dm.waitForTimeout(1500);
    combatSessionId = '';
    note(SPEC, 'GP-11', 'INFO', 'Kampf per UI beendet');
    await shot(pages.dm, 'gp11-ended');
  } else {
    note(SPEC, 'GP-11', 'WARN', 'Beenden-Button nicht gefunden (Cleanup per API)');
  }
  // P1 sieht Kampf live?
  await pages.p1.goto(`/campaigns/${IDS.campaignId}`);
  await shot(pages.p1, 'gp11-p1-view');
  reportHeuristics(SPEC, 'GP-11', await heuristics(pages.dm));
  summarizeWatch(SPEC, 'GP-11', w);
  await closePages(pages);
});

test('GP-12 Abenteuer spielen (Start, Skillcheck, Zweig)', async ({ browser }) => {
  const pages = await openPages(browser, ['p1']);
  const w = watchPage(pages.p1);
  await pages.p1.goto(`/worlds/${IDS.forkWorldId}/adventures/${IDS.adventureId}`);
  const okNode = await pages.p1.getByText(/dunklen Höhle/).isVisible({ timeout: 15_000 }).catch(() => false);
  check(SPEC, 'GP-12', okNode, 'Start-Node-Text fehlt (Auto-Start mit erstem PC?)');
  await shot(pages.p1, 'gp12-start');
  const climb = pages.p1.getByRole('button', { name: 'Hinabklettern' });
  if (await climb.isVisible({ timeout: 10_000 }).catch(() => false)) {
    const [aResp] = await Promise.all([
      pages.p1.waitForResponse((r) => r.url().includes('/advance') && r.request().method() === 'POST', { timeout: 15_000 }).catch(() => null),
      climb.click(),
    ]);
    check(SPEC, 'GP-12', !!aResp && aResp.ok(), 'Advance: keine erfolgreiche Response');
    await pages.p1.waitForTimeout(800);
    const outcome = await pages.p1.getByText(/Erfolg|Fehlgeschlagen/).first().isVisible().catch(() => false);
    check(SPEC, 'GP-12', outcome, 'Kein Erfolgs-/Fehlschlag-Indikator nach Advance');
    await shot(pages.p1, 'gp12-after-choice');
  } else {
    note(SPEC, 'GP-12', 'WARN', 'Choice-Button Hinabklettern nicht sichtbar');
  }
  summarizeWatch(SPEC, 'GP-12', w);
  await closePages(pages);
});

test('GP-13 Soziale Probe im NPC-Panel', async ({ browser, request }) => {
  const pages = await openPages(browser, ['p1']);
  const w = watchPage(pages.p1);
  await pages.p1.goto(`/worlds/${IDS.forkWorldId}/npcs/${IDS.alrikId}`);
  const panel = pages.p1.getByText(/Soziale Probe/);
  const hasPanel = await panel.isVisible({ timeout: 15_000 }).catch(() => false);
  check(SPEC, 'GP-13', hasPanel, 'Panel „Soziale Probe" fehlt (Regeln geladen? Kampagne aktiv?)');
  if (hasPanel) {
    const section = panel.locator('xpath=ancestor::section[1]');
    const actorSel = section.getByRole('combobox').first();
    if (await actorSel.isVisible().catch(() => false)) await actorSel.selectOption(IDS.miraId).catch(() => {});
    const actionSel = section.getByRole('combobox').nth(1);
    if (await actionSel.isVisible().catch(() => false)) {
      const opts = actionSel.locator('option');
      const n = await opts.count();
      for (let i = 1; i < n; i++) {
        const t = (await opts.nth(i).textContent()) ?? '';
        if (/bitten/i.test(t)) { await actionSel.selectOption({ label: t.trim() }).catch(() => {}); break; }
      }
    }
    const rollBtn = section.getByRole('button', { name: /Probe würfeln|Würfeln|Roll/ });
    if (await rollBtn.isVisible({ timeout: 5_000 }).catch(() => false)) {
      const [sResp] = await Promise.all([
        pages.p1.waitForResponse((r) => r.url().includes('/rolls/probe') && r.request().method() === 'POST', { timeout: 15_000 }).catch(() => null),
        rollBtn.click(),
      ]);
      check(SPEC, 'GP-13', !!sResp && sResp.ok(), 'Sozial-Probe: keine erfolgreiche Response');
      await pages.p1.waitForTimeout(800);
      await shot(pages.p1, 'gp13-social-result');
      const npc = await request.get(`${API}/api/v1/worlds/${IDS.forkWorldId}/entities/${IDS.alrikId}`, {
        headers: { Authorization: `Bearer ${tokenFor('dm')}` },
      });
      if (npc.ok()) {
        const meta = JSON.parse((await npc.json()).metadataJson ?? '{}');
        check(SPEC, 'GP-13', Array.isArray(meta.conditions) && meta.conditions.length > 0, 'Kein Zustand am NPC nach Sozial-Probe');
      }
    } else {
      note(SPEC, 'GP-13', 'WARN', 'Roll-Button im Sozial-Panel nicht gefunden');
    }
  }
  summarizeWatch(SPEC, 'GP-13', w);
  await closePages(pages);
});
