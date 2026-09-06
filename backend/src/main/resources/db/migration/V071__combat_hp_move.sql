-- ============================================================
-- V071__combat_hp_move.sql
-- Living World Engine — HP-Tracking + MOVE-Aktion
-- ============================================================

ALTER TABLE combat_participants
  ADD COLUMN hp_current INTEGER NOT NULL DEFAULT 10,
  ADD COLUMN hp_max     INTEGER NOT NULL DEFAULT 10;

COMMENT ON COLUMN combat_participants.hp_current IS 'Aktuelle Trefferpunkte (0 = besiegt)';
COMMENT ON COLUMN combat_participants.hp_max IS 'Maximale Trefferpunkte';
