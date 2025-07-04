ALTER TABLE cas3_void_bedspaces
    AlTER COLUMN premises_id drop not null;
ALTER TABLE cas3_void_bedspaces DROP CONSTRAINT bed_id_fk;
ALTER TABLE cas3_void_bedspaces DROP CONSTRAINT lost_beds_premises_id_fkey;
