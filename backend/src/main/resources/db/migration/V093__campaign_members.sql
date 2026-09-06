CREATE TABLE campaign_members (
    id          UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    campaign_id UUID        NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL CHECK (role IN ('PLAYER', 'DM')),
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (campaign_id, user_id)
);
CREATE INDEX idx_campaign_members_campaign ON campaign_members(campaign_id);
