-- !Ups

ALTER TABLE rules
  ADD COLUMN created_by TEXT NOT NULL DEFAULT 'Google Sheet',
  ADD COLUMN created_at timestamp DEFAULT current_timestamp,
  ADD COLUMN updated_by TEXT NOT NULL DEFAULT 'Google Sheet',
  ADD COLUMN updated_at timestamp DEFAULT current_timestamp;

-- !Downs

ALTER TABLE rules
  DROP COLUMN created_by,
  DROP COLUMN created_at,
  DROP COLUMN updated_by,
  DROP COLUMN updated_at;
