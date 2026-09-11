-- P27-T03: Kampagnen erhalten beim Erstellen eine eigene Welt-Kopie (Fork).
ALTER TABLE campaigns ADD COLUMN forked_world BOOLEAN NOT NULL DEFAULT false;
