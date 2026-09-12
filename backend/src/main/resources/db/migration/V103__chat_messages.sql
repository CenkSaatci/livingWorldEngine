-- B5: Chat-Historie (letzte 50 ladbar).
CREATE TABLE chat_messages (
    id UUID PRIMARY KEY,
    world_id UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    sender TEXT NOT NULL,
    text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_chat_messages_world ON chat_messages(world_id, created_at DESC);
