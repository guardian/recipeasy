ALTER TABLE curated_recipe
    ADD images json not null default '{"images": []}'::jsonb ;

