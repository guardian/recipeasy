
UPDATE recipe SET steps='{"steps": []}'::jsonb WHERE steps IS NULL;

ALTER TABLE recipe
  ALTER COLUMN title SET NOT NULL,
  ALTER COLUMN body SET NOT NULL,
  ALTER COLUMN ingredients_lists SET NOT NULL,
  ALTER COLUMN article_ID SET NOT NULL,
  ALTER COLUMN publication_date SET NOT NULL,
  ALTER COLUMN status SET NOT NULL,
  ALTER COLUMN steps SET NOT NULL;
