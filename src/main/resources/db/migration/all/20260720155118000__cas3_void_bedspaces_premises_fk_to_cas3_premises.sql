ALTER TABLE IF EXISTS cas3_void_bedspaces DROP CONSTRAINT IF EXISTS cas3_void_bedspaces_premises_id_fkey;
ALTER TABLE IF EXISTS cas3_void_bedspaces ADD CONSTRAINT cas3_void_bedspaces_premises_id_cas3_premises_id_fk FOREIGN KEY (premises_id) REFERENCES cas3_premises(id);
