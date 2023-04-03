-- !Ups

ALTER TABLE rules
ALTER COLUMN pattern drop NOT NULL;

-- !Downs

UPDATE rules SET pattern = '' WHERE pattern IS NULL;
ALTER TABLE rules ALTER COLUMN pattern SET NOT NULL;
