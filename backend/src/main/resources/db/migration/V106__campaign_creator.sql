-- ADR-014: Kampagnen-Ersteller fuer Leiter-Rechte (NULL = Legacy, dann Welt-Owner).
ALTER TABLE campaigns ADD COLUMN creator_id UUID REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX idx_campaigns_creator ON campaigns(creator_id);
-- Best-effort Backfill: fruehestes DM-Mitglied je Kampagne.
UPDATE campaigns c SET creator_id = (
    SELECT cm.user_id FROM campaign_members cm
    WHERE cm.campaign_id = c.id AND cm.role = 'DM'
    ORDER BY cm.joined_at ASC LIMIT 1)
WHERE c.creator_id IS NULL;
