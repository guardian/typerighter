-- !Ups

ALTER TABLE rules_draft ADD COLUMN is_archived BOOLEAN NOT NULL DEFAULT FALSE;

-- !Downs

ALTER TABLE rules_draft DROP COLUMN is_archived;