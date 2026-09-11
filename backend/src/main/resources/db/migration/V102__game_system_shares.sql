-- T33-05: System-Shares fuer INVITE_ONLY (ADR-011).
CREATE TABLE game_system_shares (
    id UUID PRIMARY KEY,
    system_id UUID NOT NULL REFERENCES game_systems(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (system_id, user_id)
);
CREATE INDEX idx_game_system_shares_user ON game_system_shares(user_id);
