create table if not exists webhook_deliveries (
    id uuid not null,
    merchant_id uuid not null,
    payment_id uuid not null,
    webhook_url varchar(255) not null,
    status varchar(50) not null,
    payload text,
    attempt_count int,
    last_attempted_at timestamptz,
    created_at timestamptz not null,

    primary key(id)
);

create index on webhook_deliveries(merchant_id);
