-- Rules Schema

-- !Ups

CREATE TYPE ruleType AS ENUM ('regex', 'languageTool');

CREATE TABLE Rules (
      id SERIAL PRIMARY KEY,
      ruleType ruleType,
      pattern text NOT NULL,
      replacement text,
      category text,
      tags text,
      description text,
      ignore boolean NOT NULL,
      notes text,
      googleSheetId text,
      forceRedRule boolean,
      advisoryRule boolean
);

-- !Downs

DROP TABLE Rules;
DROP TYPE IF EXISTS ruleType;
