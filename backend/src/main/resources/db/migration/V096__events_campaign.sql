ALTER TABLE world_events ADD COLUMN campaign_id UUID REFERENCES campaigns(id) ON DELETE SET NULL;
CREATE INDEX idx_events_campaign ON world_events(campaign_id, created_at DESC);
