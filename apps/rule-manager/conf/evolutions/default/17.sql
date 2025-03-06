-- !Ups

CREATE INDEX rules_draft_free_text_search_idx
    ON public.rules_draft
        USING gist ((coalesce(rules_draft.pattern, '') ||
                    coalesce(rules_draft.description, '') ||
                    coalesce(rules_draft.replacement, '')) gist_trgm_ops);

-- We order by pattern by default - these indices speed up the initial call to display rules
CREATE INDEX rules_draft_pattern_idx_asc ON rules_draft(pattern);
CREATE INDEX rules_draft_pattern_idx_desc ON rules_draft(pattern DESC);

-- !Downs

DROP INDEX rules_draft_free_text_search_idx;
DROP INDEX rules_draft_pattern_idx_asc;
DROP INDEX rules_draft_pattern_idx_desc;