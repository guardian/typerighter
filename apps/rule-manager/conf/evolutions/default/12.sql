-- !Ups

ALTER TABLE rules_draft
    ADD COLUMN rule_order INT UNIQUE;

UPDATE rules_draft
    SET rule_order = id;

ALTER TABLE rules_draft
    ALTER COLUMN rule_order SET NOT NULL;

-- !Downs

ALTER TABLE rules_draft
    DROP COLUMN rule_order;
