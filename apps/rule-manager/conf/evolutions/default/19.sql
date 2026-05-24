-- !Ups

ALTER TABLE user_feedback
    ADD COLUMN addressed BOOLEAN,
    ADD COLUMN last_addressed_at TIMESTAMPTZ,
    ADD COLUMN notes TEXT,
    ADD COLUMN last_addressed_by TEXT,
    DROP COLUMN stage;

-- !Downs

ALTER TABLE user_feedback
    ADD COLUMN stage TEXT NOT NULL DEFAULT '',
    DROP COLUMN last_addressed_by,
    DROP COLUMN notes,
    DROP COLUMN last_addressed_at,
    DROP COLUMN addressed;
