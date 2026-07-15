-- ============================================================
-- V070__subscription_plans.sql
-- Living World Engine — Abonnement-Pläne mit parameterisierbaren Limits
-- ============================================================
-- Alle Limits sind in der subscription_plans-Tabelle konfigurierbar.
-- Neue Pläne oder geänderte Werte brauchen kein Code-Update.
--
-- Zahlungssysteme (Stripe/PayPal) werden erst später integriert.
-- Siehe docs/BILLING.md für den Fahrplan.

CREATE TABLE subscription_plans (
    id                   UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    name                 VARCHAR(50)  UNIQUE NOT NULL,
    description          TEXT,
    price_monthly_cents  INTEGER      NOT NULL DEFAULT 0,
    price_yearly_cents   INTEGER,
    max_worlds           INTEGER      NOT NULL DEFAULT 1
                           CHECK (max_worlds = -1 OR max_worlds >= 0),
    max_members_per_world INTEGER     NOT NULL DEFAULT 5
                           CHECK (max_members_per_world = -1 OR max_members_per_world >= 0),
    max_entities_per_world INTEGER    NOT NULL DEFAULT 50
                           CHECK (max_entities_per_world = -1 OR max_entities_per_world >= 0),
    max_storage_mb       INTEGER      NOT NULL DEFAULT 100
                           CHECK (max_storage_mb = -1 OR max_storage_mb >= 0),
    ai_mode_allowed      BOOLEAN      NOT NULL DEFAULT false,
    features_json        JSONB        NOT NULL DEFAULT '{}',
    priority             INTEGER      NOT NULL DEFAULT 0,
    active               BOOLEAN      NOT NULL DEFAULT true,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE subscription_plans IS 'Abonnement-Pläne mit parameterisierbaren Limits. -1 = unlimitiert.';
COMMENT ON COLUMN subscription_plans.price_monthly_cents IS 'Preis in Cent (0 = kostenlos)';
COMMENT ON COLUMN subscription_plans.features_json IS 'Zusätzliche Feature-Flags (z.B. {"weather":true})';

-- User um Plan-Felder erweitern
ALTER TABLE users
  ADD COLUMN plan_id          UUID REFERENCES subscription_plans(id),
  ADD COLUMN plan_expires_at  TIMESTAMPTZ;

COMMENT ON COLUMN users.plan_id IS 'Aktueller Abonnement-Plan (NULL = FREE)';
COMMENT ON COLUMN users.plan_expires_at IS 'Ablaufdatum (NULL = nie, z.B. FREE/ENTERPRISE)';

-- ============================================================
-- Seed-Daten: 4 Standard-Pläne
-- ============================================================
INSERT INTO subscription_plans (id, name, description, price_monthly_cents, max_worlds,
  max_members_per_world, max_entities_per_world, max_storage_mb, ai_mode_allowed, priority)
VALUES
  (gen_random_uuid(), 'FREE',       'Für Einsteiger — eine Welt, keine KI',          0,    1,  5,   50,  100, false, 0),
  (gen_random_uuid(), 'STARTER',    'Für kleine Gruppen — KI-Vorschläge',          999,    5, 10,  200,  500, true,  1),
  (gen_random_uuid(), 'PRO',        'Für ernsthafte Kampagnen — volle KI',        2499,   25, 50, 1000, 2000, true,  2),
  (gen_random_uuid(), 'ENTERPRISE', 'Unbegrenzt — individueller Preis',               0,   -1, -1,   -1,   -1, true,  3);
