-- !Ups

ALTER TABLE rules_live
  DROP COLUMN id, -- We no longer use id as a primary key, so it's no longer necessary
  ADD COLUMN rule_order SERIAL,
  ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX rules_live_is_active ON rules_live (external_id, is_active) where is_active = true;
CREATE UNIQUE INDEX rules_live_unique_order ON rules_live (rule_order, is_active) where is_active = true;
CREATE UNIQUE INDEX rules_live_composite_pkey ON rules_live (external_id, revision_id);

DROP INDEX rules_live_external_id_index;
ALTER TABLE rules_live
  ALTER COLUMN rule_order DROP DEFAULT;
DROP SEQUENCE rules_live_rule_order_seq;

-- !Downs

DELETE FROM rules_live WHERE is_active != true; -- non-active rules would not be present in the old schema

DROP INDEX rules_live_composite_pkey;
DROP INDEX rules_live_is_active;
DROP INDEX rules_live_unique_order;

ALTER TABLE rules_live
  ADD COLUMN id SERIAL PRIMARY KEY,
  DROP COLUMN is_active,
  DROP COLUMN rule_order;

CREATE UNIQUE INDEX rules_live_external_id_index ON rules_live(external_id);