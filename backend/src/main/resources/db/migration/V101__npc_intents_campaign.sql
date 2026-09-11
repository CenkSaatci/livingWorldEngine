-- P27-T04: Intents der Kampagne zuordnen (Bot pro Kampagne).
ALTER TABLE npc_intents ADD COLUMN campaign_id UUID REFERENCES campaigns(id) ON DELETE SET NULL;
CREATE INDEX idx_npc_intents_campaign_id ON npc_intents(campaign_id);
