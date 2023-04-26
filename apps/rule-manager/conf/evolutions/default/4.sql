-- !Ups

ALTER TABLE rules ADD COLUMN revision_id INT NOT NULL DEFAULT 0;

-- !Downs

ALTER TABLE rules DROP COLUMN revision_id;
