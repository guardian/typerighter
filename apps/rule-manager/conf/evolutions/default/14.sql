-- !Ups

ALTER TABLE rules_draft
  ADD COLUMN ts_vector tsvector GENERATED ALWAYS AS (
    (setweight(to_tsvector('english', coalesce(description, '')), 'A') ||
    setweight(to_tsvector('english', coalesce(pattern, '')), 'B') ||
    setweight(to_tsvector('english', coalesce(category, '')), 'B') ||
    setweight(to_tsvector('english', coalesce(replacement, '')), 'B'))
  ) STORED;

CREATE INDEX rules_draft_ts_vector_idx ON rules_draft USING GIN (ts_vector);

-- !Downs

ALTER TABLE rules_draft DROP COLUMN ts_vector;
