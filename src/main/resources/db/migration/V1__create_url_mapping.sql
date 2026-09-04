create table url_mapping (
    id bigserial primary key,
    short_code varchar(32) not null,
    original_url varchar(2048) not null,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null,
    click_count bigint not null default 0,
    constraint uk_url_mapping_short_code unique (short_code)
);

create index idx_url_mapping_short_code on url_mapping (short_code);
