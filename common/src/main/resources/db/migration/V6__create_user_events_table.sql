
CREATE TABLE user_events (
    id serial primary key,
    event_datetime timestamp with time zone,
    user_email text,
    user_firstname text,
    user_lastname text,
    recipe_id text,
    operation_type text
);

