-- P27-T05: System-Pin per Snapshot (Option B) — laufende Kampagnen bleiben auf ihrer System-Version.
ALTER TABLE campaigns ADD COLUMN rules_json_snapshot TEXT;
ALTER TABLE campaigns ADD COLUMN game_system_version INTEGER;
