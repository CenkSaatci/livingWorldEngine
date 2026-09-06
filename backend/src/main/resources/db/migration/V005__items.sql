-- ============================================================
-- V005__items.sql
-- Living World Engine — Items-Katalog pro Welt
-- ============================================================

CREATE TABLE items (
    id              UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    world_id        UUID         REFERENCES worlds(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    type            VARCHAR(50)  NOT NULL CHECK (type IN ('WEAPON', 'ARMOR', 'CONSUMABLE', 'MISC')),
    weight          NUMERIC(10,2) NOT NULL DEFAULT 0,
    value           INTEGER      NOT NULL DEFAULT 0,
    bonuses_json    JSONB        NOT NULL DEFAULT '{}',
    metadata_json   JSONB        NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_items_world ON items(world_id);

COMMENT ON TABLE items IS 'Items-Katalog pro Welt. Definiert, welche Items existieren (Waffen, Rüstungen, Verbrauchsgüter).';
COMMENT ON COLUMN items.bonuses_json IS 'Effekte beim Equip: { "armor_class": 2, "initiative": 1, "staerke": 1 }';
COMMENT ON COLUMN items.metadata_json IS 'Zusätzliche Metadaten (Beschreibung, Bild-URL, …)';