alter table cas3_void_bedspaces
    add column if not exists cancellation_date  timestamptz null,
    add column if not exists cancellation_notes text        null,
    add column if not exists bedspace_id        uuid        null,
    alter column bed_id drop not null,
    add constraint cas3_void_bedspaces_bedspace_id_cas3_bedspaces_id_fk foreign key (bedspace_id)
        references cas3_bedspaces (id)
;
