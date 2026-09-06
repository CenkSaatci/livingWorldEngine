ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN verification_token VARCHAR(200);
ALTER TABLE users ADD COLUMN verification_token_expires_at TIMESTAMPTZ;
