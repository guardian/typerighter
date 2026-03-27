-- !Ups

DROP INDEX rules_draft_free_text_search_idx;

CREATE INDEX CONCURRENTLY rules_draft_free_text_search_idx
    ON public.rules_draft
        USING gin ((coalesce(rules_draft.pattern, '') ||  ' ' ||
                    coalesce(rules_draft.description, '') || ' ' ||
                    coalesce(rules_draft.replacement, '') || ' ' ||
                    coalesce(rules_draft.title, '')) gin_trgm_ops);

-- !Downs

DROP INDEX rules_draft_free_text_search_idx;

CREATE INDEX CONCURRENTLY rules_draft_free_text_search_idx
    ON public.rules_draft
        USING gin ((coalesce(rules_draft.pattern, '') ||  ' ' ||
                    coalesce(rules_draft.description, '') || ' ' ||
                    coalesce(rules_draft.replacement, '')) gin_trgm_ops);