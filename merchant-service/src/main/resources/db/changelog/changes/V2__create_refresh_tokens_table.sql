create table if not exists refresh_tokens (
    id uuid not null default gen_random_uuid(),
    merchant_id uuid not null,
    token_hash varchar(255) not null unique,
    revoked boolean not null,
    created_at timestamptz not null,
    expires_at timestamptz not null,

    primary key(id),
    foreign key (merchant_id) references merchants(id)
    );

create index on refresh_tokens(merchant_id);
create index on refresh_tokens(token_hash);
