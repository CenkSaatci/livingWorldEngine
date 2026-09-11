-- V097: game_system_id aus worlds entfernen (P25-T06).
-- Welten sind seit dem 3-Ebenen-Modell systemunabhängig; das Regelwerk
-- kommt aus der Kampagne (RulesLoader.loadSystemByCampaign).
-- Die UI blendet die Auswahl bereits aus (P26-T02); dieser Schritt
-- entfernt Spalte, API-Parameter und Domain-Feld.
ALTER TABLE worlds DROP COLUMN IF EXISTS game_system_id;
