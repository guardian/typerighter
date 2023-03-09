-- Rules Schema

-- !Ups

CREATE TYPE ruleType AS ENUM ('regex', 'languageTool');

CREATE TABLE Rules (
      id SERIAL PRIMARY KEY,
      rule_type ruleType,
      pattern text NOT NULL,
      replacement text,
      category text,
      tags text,
      description text,
      ignore boolean NOT NULL,
      notes text,
      google_sheet_id text,
      force_red_rule boolean,
      advisory_rule boolean
);

-- !Downs

DROP TABLE Rules;
DROP TYPE IF EXISTS ruleType;
