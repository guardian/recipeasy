CREATE TABLE curated_recipe(
    id varchar(32) primary key,
    title text not null,
    body text not null,
    serves jsonb null,
    ingredients_lists jsonb not null,
    article_id text not null,
    credit text null,
    publication_date timestamp with time zone not null,
    status text not null,
    times jsonb not null,
    steps jsonb not null,
    tags jsonb not null
);

