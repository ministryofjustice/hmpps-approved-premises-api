-- Delete all beds belonging to temporary-accommodation CAS3 premises from beds table
ALTER TABLE cas3_void_bedspaces DROP COLUMN bed_id;

ALTER TABLE bed_moves RENAME TO archive_bed_moves;
ALTER INDEX bed_moves_pkey RENAME TO archive_bed_moves_pkey;

DELETE FROM beds
WHERE room_id IN (
  SELECT id FROM rooms WHERE premises_id IN (
    SELECT id FROM premises WHERE service = 'temporary-accommodation'
  )
);



