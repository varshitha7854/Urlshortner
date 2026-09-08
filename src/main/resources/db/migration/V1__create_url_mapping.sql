create table url_mapping (
    id bigint not null auto_increment,
    short_code varchar(32) not null,
    original_url varchar(2048) not null,
    created_at timestamp not null default current_timestamp,
    expires_at timestamp not null,
    click_count bigint not null default 0,
    primary key (id),
    constraint uk_url_mapping_short_code unique (short_code)
);

create index idx_url_mapping_short_code
on url_mapping (short_code);