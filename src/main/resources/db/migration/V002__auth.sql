-- ============================================================
-- V002__auth.sql
-- Living World Engine — JWT Refresh-Token Persistenz
-- ============================================================

CREATE TABLE refresh_tokens (
    id              UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(64)  NOT NULL,
    expires_at      TIMESTAMPTZ  NOT NULL,
    revoked         BOOLEAN      NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);

COMMENT ON TABLE refresh_tokens IS 'Persistierte Refresh-Tokens für JWT-Rotation und Widerruf. Siehe docs/ADR/008-api-versioning.md';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'SHA-256-Hash des Refresh-Tokens (Token selbst wird nie im Klartext gespeichert)';