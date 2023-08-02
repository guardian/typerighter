-- !Ups
CREATE EXTENSION pg_trgm;
CREATE INDEX trgm_idx ON rules_draft USING GIN (pattern gin_trgm_ops);

-- !Downs
DROP EXTENSION pg_trgm;
DROP INDEX trgm_idx;