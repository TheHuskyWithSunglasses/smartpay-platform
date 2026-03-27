create table if not exists merchants (
    id uuid primary key,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    business_name varchar(255) not null,
    status varchar(50) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
)