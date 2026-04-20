create table if not exists merchants (
    id uuid not null default gen_random_uuid(),
    email varchar(255) unique not null,
    password_hash varchar(255) not null,
    business_name varchar(255) not null,
    status varchar(50) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,

    primary key(id)
    );

create index on merchants(email);
