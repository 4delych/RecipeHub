create table users (
    id uuid primary key default gen_random_uuid(),
    name varchar(100) not null,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    created_at timestamp with time zone not null default current_timestamp
);

create table recipes (
    id uuid primary key default gen_random_uuid(),
    title varchar(150) not null,
    description text,
    ingredients text not null,
    instructions text not null,
    author_id uuid not null,
    created_at timestamp with time zone not null default current_timestamp,
    constraint fk_recipes_author
        foreign key (author_id)
        references users (id)
        on delete cascade
);

create index idx_recipes_author_id on recipes (author_id);
