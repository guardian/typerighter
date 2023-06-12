-- !Ups

-- Add tags table
CREATE TABLE tags (
  id SERIAL PRIMARY KEY,
  name text NOT NULL
);

-- !Downs

DROP TABLE tags;
