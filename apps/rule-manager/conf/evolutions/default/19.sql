-- !Ups

ALTER TABLE user_feedback RENAME COLUMN rule_id TO external_rule_id;
ALTER TABLE user_feedback ADD COLUMN rule_id INT REFERENCES rules_draft(id);

CREATE INDEX idx_rules_draft_external_id ON rules_draft(external_id);

-- !Downs

DROP INDEX idx_rules_draft_external_id;

ALTER TABLE user_feedback DROP COLUMN rule_id;
ALTER TABLE user_feedback RENAME COLUMN external_rule_id TO rule_id;
