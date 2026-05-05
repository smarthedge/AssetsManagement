-- Social login: make password_hash nullable, add provider columns
ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS provider VARCHAR(50) NULL,
    ADD COLUMN IF NOT EXISTS provider_account_id VARCHAR(255) NULL;

ALTER TABLE users
    ADD CONSTRAINT uq_users_provider_account
        UNIQUE (provider, provider_account_id);

-- Password reset: token + expiry
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_reset_token VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS password_reset_expires TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS idx_users_password_reset_token
    ON users (password_reset_token)
    WHERE password_reset_token IS NOT NULL;
