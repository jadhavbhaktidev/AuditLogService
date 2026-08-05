create sequence if not exists redaction_audit_seq start with 1 increment by 1;

create table if not exists redaction_audit (
    id bigint primary key,
    sequence_number bigint not null,
    redacted_fields text not null,
    redaction_reason varchar(512) not null,
    approved_by varchar(255) not null,
    approved_at timestamptz not null,
    proof_artifact text not null,
    created_at timestamptz not null default now()
);

create index if not exists idx_redaction_audit_sequence_number on redaction_audit (sequence_number);
create index if not exists idx_redaction_audit_approved_at on redaction_audit (approved_at);
