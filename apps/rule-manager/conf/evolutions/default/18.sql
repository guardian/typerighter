-- !Ups

ALTER TABLE rules_draft ADD COLUMN IF NOT EXISTS title TEXT;
ALTER TABLE rules_live ADD COLUMN IF NOT EXISTS title TEXT;

-- !Downs

ALTER TABLE rules_draft DROP COLUMN title;
ALTER TABLE rules_live DROP COLUMN title;
