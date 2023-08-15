-- !Ups
CREATE EXTENSION pg_trgm;
CREATE INDEX trgm_idx ON rules_draft USING GIN (pattern gin_trgm_ops);

ALTER TABLE rule_tag_draft
    DROP CONSTRAINT fk_rule_id,
    ADD CONSTRAINT fk_rule_id FOREIGN KEY (rule_id) REFERENCES rules_draft(id) ON DELETE CASCADE;

ALTER TABLE rule_tag_live
    DROP CONSTRAINT fk_rule_id,
    ADD CONSTRAINT fk_rule_id FOREIGN KEY (rule_external_id, rule_revision_id) REFERENCES rules_live(external_id, revision_id) ON DELETE CASCADE;

-- !Downs
DROP INDEX trgm_idx;
DROP EXTENSION pg_trgm;
