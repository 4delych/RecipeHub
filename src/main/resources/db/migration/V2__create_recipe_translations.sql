create table recipe_translations (
    id uuid primary key default gen_random_uuid(),
    recipe_id uuid not null,
    language_code varchar(10) not null,
    title text not null,
    description text,
    ingredients text not null,
    instructions text not null,
    created_at timestamp with time zone not null default current_timestamp,
    constraint fk_recipe_translations_recipe
        foreign key (recipe_id)
        references recipes (id)
        on delete cascade,
    constraint uq_recipe_translations_recipe_id_language_code
        unique (recipe_id, language_code)
);
