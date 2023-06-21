-- !Ups

ALTER TABLE rules_draft DROP COLUMN tags;
ALTER TABLE rules_live DROP COLUMN tags;

ALTER TABLE rule_tag_draft
    ADD CONSTRAINT fk_rule_id FOREIGN KEY (rule_id) REFERENCES rules_draft(id),
    ADD CONSTRAINT fk_tag_id FOREIGN KEY (tag_id) REFERENCES tags(id);

ALTER TABLE rule_tag_live
    ADD CONSTRAINT fk_rule_id FOREIGN KEY (rule_id) REFERENCES rules_live(id),
    ADD CONSTRAINT fk_tag_id FOREIGN KEY (tag_id) REFERENCES tags(id);

-- !Downs

ALTER TABLE rule_tag_draft
    DROP CONSTRAINT fk_rule_id,
    DROP CONSTRAINT fk_tag_id;

ALTER TABLE rule_tag_live
    DROP CONSTRAINT fk_rule_id,
    DROP CONSTRAINT fk_tag_id;