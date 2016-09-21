CREATE TABLE curated_recipe(
    -- NB id not consistent with recipe table which uses varchar(32) :( --
    id serial primary key,
    recipe_id varchar(32) REFERENCES recipe (id),
    title text not null,
    serves jsonb null,
    ingredients_lists jsonb not null,
    credit text null,
    times jsonb not null,
    steps jsonb not null,
    tags jsonb not null
);
