
-- giving up on enum for now as it doesn't play nicely with Quill
-- CREATE TYPE status AS ENUM ('New', 'Curated', 'Impossible');

CREATE TABLE recipe (
    id varchar(32) primary key,
    title text,
    body text,
    serves jsonb null,
    ingredients_lists jsonb,
    article_id text,
    credit text null,
    publication_date timestamp with time zone,
    status text,
    steps jsonb
);

