-- !Ups

-- Add live rules table, dropping rules marked as ignore
CREATE TABLE rules_live (LIKE rules INCLUDING DEFAULTS);

ALTER TABLE rules_live
    DROP COLUMN id,
    ADD COLUMN id SERIAL PRIMARY KEY,
    ADD COLUMN reason TEXT DEFAULT 'First published';

ALTER TABLE rules_live
    DROP COLUMN ignore;

-- Rename existing table to draft rules table
ALTER TABLE rules
    RENAME TO rules_draft;

-- !Downs

ALTER TABLE rules_draft
    RENAME TO rules;

DROP TABLE rules_live;
