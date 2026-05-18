-- !Ups

ALTER TABLE user_feedback
    ADD COLUMN actioned BOOLEAN,
    ADD COLUMN actioned_at TIMESTAMPTZ,
    ADD COLUMN action_type TEXT,
    ADD COLUMN action_notes TEXT;

-- !Downs

ALTER TABLE user_feedback
    DROP COLUMN action_notes,
    DROP COLUMN action_type,
    DROP COLUMN actioned_at,
    DROP COLUMN actioned;
