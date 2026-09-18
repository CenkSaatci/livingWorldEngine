-- R3/Block C: Optimistic Locking fuer Kampf-Teilnehmer
-- (verhindert Lost Updates / AP-Vervielfachung bei parallelen Requests, z. B. Doppel-Submit)
ALTER TABLE combat_participants ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
