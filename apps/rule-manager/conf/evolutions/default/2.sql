-- !Ups

ALTER TABLE rules
ALTER COLUMN pattern drop NOT NULL;

-- !Downs

ALTER TABLE rules
ALTER COLUMN pattern add NOT NULL;
