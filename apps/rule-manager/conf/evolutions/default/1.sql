-- Rules Schema

-- !Ups

CREATE TABLE rules (
      id SERIAL PRIMARY KEY,
      rule_type text NOT NULL,
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

DROP TABLE rules;
