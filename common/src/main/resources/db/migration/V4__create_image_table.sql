CREATE TABLE image (
    -- cannot use media_id as primary key as some images are used twice --
    id serial primary key,
    media_id varchar(80) not null,
    article_id text not null,
    asset_url text not null,
    alt_text text
);

