-- Remove created_by_user_id and last_changed_by_user_id from all entity tables
ALTER TABLE users
    DROP COLUMN IF EXISTS created_by_user_id,
    DROP COLUMN IF EXISTS last_changed_by_user_id;

ALTER TABLE roles
    DROP COLUMN IF EXISTS created_by_user_id,
    DROP COLUMN IF EXISTS last_changed_by_user_id;

ALTER TABLE assets
    DROP COLUMN IF EXISTS created_by_user_id,
    DROP COLUMN IF EXISTS last_changed_by_user_id;
