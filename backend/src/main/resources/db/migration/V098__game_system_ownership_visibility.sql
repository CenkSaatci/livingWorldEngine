-- P27-T01 / F8: Ownership + Visibility fuer Game-Systeme und Welten.
-- Bestands-Systeme: Owner = erster Admin (sonst aeltester User), Sichtbarkeit PUBLIC,
-- damit bestehende globale Systeme weiter nutzbar bleiben. Owner NULL = Legacy-global (nur Admin schreibbar).
ALTER TABLE game_systems ADD COLUMN owner_id UUID REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE game_systems ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE'
    CHECK (visibility IN ('PRIVATE', 'INVITE_ONLY', 'PUBLIC'));
CREATE INDEX idx_game_systems_owner_id ON game_systems(owner_id);
CREATE INDEX idx_game_systems_visibility ON game_systems(visibility);

UPDATE game_systems SET owner_id = COALESCE(
    (SELECT id FROM users WHERE role = 'ADMIN' ORDER BY created_at LIMIT 1),
    (SELECT id FROM users ORDER BY created_at LIMIT 1)
);
UPDATE game_systems SET visibility = 'PUBLIC';

-- Welten: bestehendes Verhalten = Owner + eingeladene Mitglieder -> INVITE_ONLY.
-- PUBLIC-Lesepfad folgt mit P27-T01-d (requireRead); PRIVATE = nur Owner.
ALTER TABLE worlds ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'INVITE_ONLY'
    CHECK (visibility IN ('PRIVATE', 'INVITE_ONLY', 'PUBLIC'));
CREATE INDEX idx_worlds_visibility ON worlds(visibility);
