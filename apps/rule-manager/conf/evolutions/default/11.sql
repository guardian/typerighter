-- !Ups

ALTER TABLE rules_draft DROP COLUMN tags;
ALTER TABLE rules_live
    DROP COLUMN tags;

ALTER TABLE rule_tag_draft
    ADD CONSTRAINT fk_rule_id FOREIGN KEY (rule_id) REFERENCES rules_draft(id),
    ADD CONSTRAINT fk_tag_id FOREIGN KEY (tag_id) REFERENCES tags(id);

ALTER TABLE rule_tag_live
  RENAME COLUMN rule_id TO rule_external_id;

ALTER TABLE rule_tag_live
    ALTER COLUMN rule_external_id SET DATA TYPE TEXT,
    ADD COLUMN rule_revision_id INT NOT NULL,
    DROP CONSTRAINT rule_tag_live_rule_id_tag_id_key,
    ADD CONSTRAINT rule_tag_live_unique UNIQUE (rule_external_id, rule_revision_id, tag_id),
    ADD CONSTRAINT fk_rule_id FOREIGN KEY (rule_external_id, rule_revision_id) REFERENCES rules_live(external_id, revision_id),
    ADD CONSTRAINT fk_tag_id FOREIGN KEY (tag_id) REFERENCES tags(id);

-- !Downs

ALTER TABLE rule_tag_draft
    DROP CONSTRAINT fk_rule_id,
    DROP CONSTRAINT fk_tag_id;

ALTER TABLE rule_tag_live
    DROP CONSTRAINT fk_rule_id,
    DROP CONSTRAINT fk_tag_id,
    DROP CONSTRAINT rule_tag_live_unique,
    ADD CONSTRAINT rule_tag_live_rule_id_tag_id_key UNIQUE (rule_external_id, tag_id),
    DROP COLUMN rule_revision_id;

ALTER TABLE rule_tag_live
  RENAME COLUMN rule_external_id TO rule_id;
