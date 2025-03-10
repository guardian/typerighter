-- !Ups

CREATE INDEX rules_draft_free_text_search_idx
    ON public.rules_draft
        USING gin ((coalesce(rules_draft.pattern, '') ||
                    coalesce(rules_draft.description, '') ||
                    coalesce(rules_draft.replacement, '')) gin_trgm_ops);

-- We order by updated_at by default - this index speeds up the initial call to display rules
CREATE INDEX CONCURRENTLY rules_draft_updated_at ON rules_draft(updated_at DESC);

-- `left` is used to index on a substring, as patterns can be very large, and there is a limit
-- on the size of the data that can cause write errors (https://stackoverflow.com/a/70124913)
CREATE INDEX CONCURRENTLY rules_draft_pattern_idx_asc ON rules_draft(left(pattern, 20));
CREATE INDEX CONCURRENTLY rules_draft_pattern_idx_desc ON rules_draft(left(pattern, 20) DESC);

-- !Downs

DROP INDEX rules_draft_free_text_search_idx;
DROP INDEX rules_draft_updated_at;
DROP INDEX rules_draft_pattern_idx_asc;
DROP INDEX rules_draft_pattern_idx_desc;
