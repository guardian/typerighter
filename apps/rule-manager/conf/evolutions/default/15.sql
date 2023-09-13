-- !Ups

CREATE INDEX rules_draft_rule_type ON rules_draft (rule_type);

-- !Downs

DROP INDEX rules_draft_rule_type;
