-- ============================================================
-- V001__initial.sql
-- Living World Engine — initiales Schema
-- Enthält: users, game_systems, worlds, world_members, entities,
--          world_events, npc_intents
-- ============================================================

-- Alle Tabellen haben created_at/updated_at Audit-Spalten.
-- JSONB-Spalten tragen das Suffix _json.
-- Primary Keys sind UUID DEFAULT gen_random_uuid().

-- ============================================================
-- 1. Users
-- ============================================================
CREATE TABLE users (
    id              UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    email           VARCHAR(200) UNIQUE NOT NULL,
    username        VARCHAR(100) UNIQUE NOT NULL,
    password_hash   VARCHAR(200) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'USER'
                            CHECK (role IN ('USER', 'ADMIN')),
    locale          VARCHAR(10) NOT NULL DEFAULT 'de',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_users_role ON users(role);

COMMENT ON COLUMN users.locale IS 'BCP 47-Tag (z. B. de, en, fr). Siehe docs/ADR/007-internationalization-strategy.md';

-- ============================================================
-- 2. Game Systems (Regelwerke)
-- ============================================================
CREATE TABLE game_systems (
    id              UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    version         INTEGER     NOT NULL DEFAULT 1,
    rules_json      JSONB       NOT NULL,
    schema_json     JSONB       NOT NULL,
    active          BOOLEAN     NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_game_systems_name ON game_systems(name);

-- ============================================================
-- 3. Worlds (Spielwelten, Multi-Tenant-Schlüssel ist owner_id)
-- ============================================================
CREATE TABLE worlds (
    id                  UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    name                VARCHAR(200) NOT NULL,
    owner_id            UUID        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    game_system_id      UUID        REFERENCES game_systems(id) ON DELETE SET NULL,
    settings_json       JSONB       NOT NULL DEFAULT '{}',
    current_game_time   TIMESTAMPTZ NULL,
    last_tick_at        TIMESTAMPTZ NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_worlds_owner_id ON worlds(owner_id);
CREATE INDEX idx_worlds_game_system_id ON worlds(game_system_id);

COMMENT ON COLUMN worlds.current_game_time IS 'In-Game-Zeit der Welt. Siehe docs/ADR/009-world-time-calendar-system.md';
COMMENT ON COLUMN worlds.last_tick_at IS 'Echtzeitstempel des letzten automatischen Ticks der Time Engine.';

-- ============================================================
-- 4. World Members (Mitglieder einer Welt: DM + Spieler)
-- ============================================================
CREATE TABLE world_members (
    id              UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    world_id        UUID        NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL CHECK (role IN ('PLAYER', 'DM')),
    joined_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (world_id, user_id)
);

-- ============================================================
-- 5. Entities (PC, NPC, FRAKTION — universelle Tabelle)
-- ============================================================
CREATE TABLE entities (
    id              UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    world_id        UUID        NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    entity_type     VARCHAR(20) NOT NULL CHECK (entity_type IN ('PC', 'NPC', 'FACTION')),
    name            VARCHAR(200) NOT NULL,
    attributes_json JSONB       NOT NULL DEFAULT '{}',
    inventory_json  JSONB       NOT NULL DEFAULT '[]',
    position_json   JSONB       NULL,
    metadata_json   JSONB       NOT NULL DEFAULT '{}',
    faction_id      UUID        REFERENCES entities(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_entities_world_id ON entities(world_id);
CREATE INDEX idx_entities_type ON entities(entity_type);
CREATE INDEX idx_entities_faction_id ON entities(faction_id);
CREATE INDEX idx_entities_attributes ON entities USING GIN (attributes_json);
CREATE INDEX idx_entities_metadata ON entities USING GIN (metadata_json);

-- ============================================================
-- 6. World Events (Event-Log — History für KI-Bot)
-- ============================================================
CREATE TABLE world_events (
    id                  BIGSERIAL    PRIMARY KEY,
    world_id            UUID         NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    event_type          VARCHAR(50)  NOT NULL,
    source_entity_id    UUID         REFERENCES entities(id) ON DELETE SET NULL,
    target_entity_id    UUID         REFERENCES entities(id) ON DELETE SET NULL,
    payload_json        JSONB        NOT NULL DEFAULT '{}',
    event_hash          VARCHAR(64)  NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_events_world_time ON world_events(world_id, created_at DESC);
CREATE INDEX idx_events_type ON world_events(world_id, event_type);

-- ============================================================
-- 7. NPC Intents (KI-Vorschläge, die der Server validiert)
-- ============================================================
CREATE TABLE npc_intents (
    id              UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    world_id        UUID        NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    npc_id          UUID        NOT NULL REFERENCES entities(id) ON DELETE CASCADE,
    intent_type     VARCHAR(50) NOT NULL CHECK (intent_type IN ('ATTACK', 'MOVE', 'SPEAK', 'USE_ITEM', 'IDLE')),
    params_json     JSONB       NOT NULL,
    reasoning       TEXT        NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'pending'
                    CHECK (status IN ('pending', 'approved', 'rejected', 'executed', 'failed')),
    rejection_reason TEXT       NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    validated_at    TIMESTAMPTZ NULL
);
CREATE INDEX idx_intents_world_status ON npc_intents(world_id, status);
CREATE INDEX idx_intents_created_at ON npc_intents(created_at);
