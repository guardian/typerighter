-- !Ups

DROP INDEX trgm_idx;
CREATE INDEX rules_draft_pattern_trgm_idx ON rules_draft USING GIN (pattern gin_trgm_ops);
CREATE INDEX rules_draft_description_trgm_idx ON rules_draft USING GIN (description gin_trgm_ops);
CREATE INDEX rules_draft_category_trgm_idx ON rules_draft USING GIN (category gin_trgm_ops);
CREATE INDEX rules_draft_replacement_trgm_idx ON rules_draft USING GIN (replacement gin_trgm_ops);

-- !Downs

DROP INDEX rules_draft_pattern_trgm_idx;
DROP INDEX rules_draft_description_trgm_idx;
DROP INDEX rules_draft_category_trgm_idx;
DROP INDEX rules_draft_replacement_trgm_idx;

CREATE INDEX trgm_idx ON rules_draft USING GIN (pattern gin_trgm_ops);
