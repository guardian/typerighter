-- !Ups

ALTER TABLE rules
  ADD COLUMN created_by TEXT NOT NULL DEFAULT 'Google Sheet',
  ADD COLUMN created_at timestamptz DEFAULT now(),
  ADD COLUMN updated_by TEXT NOT NULL DEFAULT 'Google Sheet',
  ADD COLUMN updated_at timestamptz DEFAULT now();

-- !Downs

ALTER TABLE rules
  DROP COLUMN created_by,
  DROP COLUMN created_at,
  DROP COLUMN updated_by,
  DROP COLUMN updated_at;
