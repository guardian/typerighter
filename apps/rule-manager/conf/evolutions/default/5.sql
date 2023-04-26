-- !Ups

ALTER TABLE rules
  RENAME COLUMN google_sheet_id TO external_id;

ALTER TABLE rules
  ALTER COLUMN external_id SET DEFAULT gen_random_uuid()::text,
  ALTER COLUMN external_id SET NOT NULL;

CREATE UNIQUE INDEX rules_external_id_index ON rules(external_id);

-- !Downs

DROP INDEX rules_external_id_index;

ALTER TABLE rules
  ALTER COLUMN external_id drop NOT NULL;

ALTER TABLE rules
  RENAME COLUMN external_id TO google_sheet_id;
