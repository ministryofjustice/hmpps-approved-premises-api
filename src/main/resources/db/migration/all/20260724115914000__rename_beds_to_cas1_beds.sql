ALTER TABLE beds RENAME TO cas1_beds;

-- Align the index/primary-key names on the (now renamed) cas1_beds table
ALTER INDEX IF EXISTS beds_pkey RENAME TO cas1_beds_pkey;
ALTER INDEX IF EXISTS beds_code_key RENAME TO cas1_beds_code_key;
ALTER INDEX IF EXISTS beds_room_id_idx RENAME TO cas1_beds_room_id_idx;
