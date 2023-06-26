-- !Ups

ALTER TABLE rules_draft DROP COLUMN tags;
ALTER TABLE rules_live
    DROP COLUMN tags;

ALTER TABLE rule_tag_draft
    ADD CONSTRAINT fk_rule_id FOREIGN KEY (rule_id) REFERENCES rules_draft(id),
    ADD CONSTRAINT fk_tag_id FOREIGN KEY (tag_id) REFERENCES tags(id);

ALTER TABLE rule_tag_live
    ALTER COLUMN rule_id SET DATA TYPE TEXT,
    ADD COLUMN revision_id INT NOT NULL DEFAULT 0,
    ADD CONSTRAINT fk_rule_id FOREIGN KEY (rule_id, revision_id) REFERENCES rules_live(external_id, revision_id),
    ADD CONSTRAINT fk_tag_id FOREIGN KEY (tag_id) REFERENCES tags(id);

-- !Downs

ALTER TABLE rule_tag_draft
    DROP CONSTRAINT fk_rule_id,
    DROP CONSTRAINT fk_tag_id;

ALTER TABLE rule_tag_live
    DROP CONSTRAINT fk_rule_id,
    DROP CONSTRAINT fk_tag_id;

ALTER TABLE rules_live
    DROP CONSTRAINT unique_external_id;