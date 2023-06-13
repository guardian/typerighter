-- !Ups

-- Add rules_live to tags join table
CREATE TABLE rules_live_tags (
    rule_id integer NOT NULL,
    tag_id integer NOT NULL,
    UNIQUE (rule_id, tag_id)
);

-- Add rules_draft to tags join table
CREATE TABLE rules_draft_tags (
  rule_id integer NOT NULL,
  tag_id integer NOT NULL,
  UNIQUE (rule_id, tag_id)
);

-- !Downs

DROP TABLE rules_live_tags;
DROP TABLE rules_draft_tags;
