-- !Ups

-- Add live rules table, dropping rules marked as ignore
CREATE TABLE rules_live
    AS SELECT * FROM rules WHERE ignore = false;

ALTER TABLE rules_live
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