create sequence if not exists audit_records_seq start with 1 increment by 1;

create table if not exists audit_records (
    id bigint primary key,
    sequence_number bigint not null unique,
    event_type varchar(100) not null,
    actor_id varchar(255) not null,
    resource_type varchar(255) not null,
    resource_id varchar(255) not null,
    payload_json text not null,
    event_timestamp bigint not null,
    ingestion_timestamp timestamptz not null default now(),
    prev_hash varchar(64) not null,
    record_hash varchar(64) not null,
    chain_version integer not null default 1,
    redaction_state varchar(32) not null default 'NONE',
    archived_at timestamptz null,
    created_at timestamptz not null default now()
);

create index if not exists idx_audit_records_actor_id on audit_records (actor_id);
create index if not exists idx_audit_records_resource on audit_records (resource_type, resource_id);
create index if not exists idx_audit_records_event_type on audit_records (event_type);
create index if not exists idx_audit_records_event_timestamp on audit_records (event_timestamp);
create index if not exists idx_audit_records_sequence_number on audit_records (sequence_number);
