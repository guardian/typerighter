-- !Ups

-- Initialise existing rows with the default user 'Google Sheet',
-- which will be the source of any existing rows.
ALTER TABLE rules
  ADD COLUMN created_by TEXT NOT NULL DEFAULT 'Google Sheet',
  ADD COLUMN created_at timestamptz DEFAULT now(),
  ADD COLUMN updated_by TEXT NOT NULL DEFAULT 'Google Sheet',
  ADD COLUMN updated_at timestamptz DEFAULT now();

-- Remove that default for future rows.
ALTER TABLE rules
  ALTER COLUMN created_by DROP DEFAULT,
  ALTER COLUMN updated_by DROP DEFAULT;

-- !Downs

ALTER TABLE rules
  DROP COLUMN created_by,
  DROP COLUMN created_at,
  DROP COLUMN updated_by,
  DROP COLUMN updated_at;
