# Billing / Subscription Integration

> Status: **Geplant** — Noch nicht implementiert.
> Alle Limit-Prüfungen funktionieren aktuell mit den Seed-Daten aus `subscription_plans`.
> Manuelle Plan-Zuweisungen sind über `PATCH /api/v1/admin/users/{id}/plan` möglich.

## Aktueller Stand

- **subscription_plans** Tabelle mit parameterisierbaren Limits (FREE/STARTER/PRO/ENTERPRISE)
- **QuotaService** prüft Limits bei Welten-Erstellung und Member-Einladung
- **Admin-Endpunkte** zum Verwalten von Plänen und User-Zuweisungen
- **Kein Zahlungssystem** integriert — alle Plan-Zuweisungen erfolgen manuell über Admin-API

## Fahrplan

### Phase 1 — Manuelle Plan-Verwaltung (✅ erledigt)
- [x] subscription_plans Tabelle + Seed-Daten
- [x] QuotaService mit parameterisierbaren Limits
- [x] Welten-Limit + Member-Limit in WorldService
- [x] Admin-API: Pläne CRUD + User-Plan setzen

### Phase 2 — Stripe-Integration (🔜 Zukunft)
- [ ] `stripe_customer_id` auf users
- [ ] `POST /api/v1/billing/create-checkout-session`
- [ ] `POST /api/v1/billing/webhook` (Stripe-Events)
- [ ] Automatische Plan-Zuweisung bei erfolgreicher Zahlung
- [ ] Ablauf-Datum (`plan_expires_at`) setzen bei nicht-verlängerten Abos
- [ ] Stripe Customer Portal für eigenständige Plan-Verwaltung

### Phase 3 — Erweiterte Limits & Feature-Gates (🔜 Zukunft)
- [ ] Feature-Flags via `features_json` auswerten
- [ ] KI-Modus auf Plan-Ebende gaten
- [ ] Speicher-Limit (`max_storage_mb`) durchsetzen
- [ ] Entitäten-Limit (`max_entities_per_world`) durchsetzen

### Phase 4 — Abrechnung & Analytics (🔜 Zukunft)
- [ ] Monatliche Usage-Reports
- [ ] Admin-Dashboard mit Plan-Verteilung
- [ ] Downgrade-Benachrichtigungen bei Limit-Überschreitung
- [ ] Pro-Rata-Upgrades/Downgrades

## Architektur

```
User → plan_id → subscription_plans
                        ↓
                  QuotaService.check*()
                        ↓
                  WorldService.create() / addMember()
                        ↓
                  QuotaException → GlobalExceptionHandler → 403
```

## Konfiguration

Neue Pläne oder geänderte Limits benötigen **kein Code-Update**:
```sql
-- Neuen Plan erstellen (z.B. "TEAM")
INSERT INTO subscription_plans (name, max_worlds, max_members_per_world, ...)
VALUES ('TEAM', 10, 20, ...);

-- Bestehenden Plan aktualisieren
UPDATE subscription_plans SET max_worlds = 3 WHERE name = 'FREE';
```
