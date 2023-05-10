-- !Ups

ALTER TABLE rules_draft
  RENAME COLUMN google_sheet_id TO external_id;

ALTER TABLE rules_draft
  ALTER COLUMN external_id SET DEFAULT gen_random_uuid()::text,
  ALTER COLUMN external_id SET NOT NULL;

CREATE UNIQUE INDEX rules_draft_external_id_index ON rules_draft(external_id);

ALTER TABLE rules_live
  RENAME COLUMN google_sheet_id TO external_id;

ALTER TABLE rules_live
  ALTER COLUMN external_id SET NOT NULL;

CREATE UNIQUE INDEX rules_live_external_id_index ON rules_live(external_id);

-- !Downs

DROP INDEX rules_draft_external_id_index;

ALTER TABLE rules_draft
  ALTER COLUMN external_id drop NOT NULL;

ALTER TABLE rules_draft
  RENAME COLUMN external_id TO google_sheet_id;

DROP INDEX rules_live_external_id_index;

ALTER TABLE rules_live
  ALTER COLUMN external_id drop NOT NULL;

ALTER TABLE rules_live
  RENAME COLUMN external_id TO google_sheet_id;
