-- !Ups

-- Add rules_live to tags join table
CREATE TABLE rule_tag_live (
    rule_id integer NOT NULL,
    tag_id integer NOT NULL,
    UNIQUE (rule_id, tag_id)
);

-- Add rules_draft to tags join table
CREATE TABLE rule_tag_draft (
  rule_id integer NOT NULL,
  tag_id integer NOT NULL,
  UNIQUE (rule_id, tag_id)
);

-- !Downs

DROP TABLE rule_tag_live;
DROP TABLE rule_tag_draft;
