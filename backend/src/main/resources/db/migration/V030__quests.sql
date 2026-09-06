-- ============================================================
-- V030__quests.sql
-- Living World Engine — Einfache Quest-Generierung
-- ============================================================
-- Quests können vom KI-Bot vorgeschlagen und vom DM freigegeben,
-- oder vom DM manuell erstellt werden.
--
-- Siehe docs/WORLD-DEPTH.md

CREATE TABLE quests (
    id              UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    world_id        UUID         NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    type            VARCHAR(50)  NOT NULL DEFAULT 'kill'
                    CHECK (type IN ('kill', 'fetch', 'escort', 'deliver', 'explore', 'talk')),
    giver_id        UUID,                    -- NPC / Entity, der die Quest gibt
    location_id     UUID,                    -- Ort der Quest-Quelle
    objectives      JSONB        NOT NULL DEFAULT '[]',
    rewards         JSONB        NOT NULL DEFAULT '{}',
    status          VARCHAR(20)  NOT NULL DEFAULT 'active'
                    CHECK (status IN ('active', 'completed', 'failed', 'cancelled')),
    accepted_by     UUID[],
    ai_generated    BOOLEAN      NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_quests_world ON quests(world_id);
CREATE INDEX idx_quests_status ON quests(status);

COMMENT ON TABLE quests IS 'Quests — Aufträge für Spieler. Können vom KI-Bot generiert oder vom DM erstellt werden.';
COMMENT ON COLUMN quests.objectives IS 'Ziele: [{ "type": "kill", "target": "goblin", "count": 5 }]';
COMMENT ON COLUMN quests.rewards IS 'Belohnungen: { "xp": 100, "gold": 50 }';
COMMENT ON COLUMN quests.ai_generated IS 'true = vom KI-Bot vorgeschlagen und vom DM freigegeben';