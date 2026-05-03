ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE;

UPDATE app_users
SET created_at = NOW()
WHERE created_at IS NULL;

ALTER TABLE app_users
    ALTER COLUMN created_at SET DEFAULT NOW(),
    ALTER COLUMN created_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_app_users_created_at ON app_users (created_at);
