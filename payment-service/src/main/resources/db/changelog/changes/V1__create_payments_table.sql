create table if not exists payments (
    id uuid not null,
    merchant_id uuid not null,
    idempotency_key varchar(255) unique not null,
    amount bigint not null,
    currency char(3) not null,
    status varchar(50) not null,
    description text,
    created_at timestamptz not null,
    processed_at timestamptz,

    primary key(id)
);

create index on payments(merchant_id);
create index on payments(created_at);
create index on payments(merchant_id, status);
create index on payments(status) where status not in ('COMPLETED', 'FAILED');
