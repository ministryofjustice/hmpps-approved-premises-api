TRUNCATE cas1_delius_booking_import;

ALTER TABLE cas1_delius_booking_import ADD COLUMN id uuid NOT NULL;
ALTER TABLE cas1_delius_booking_import DROP CONSTRAINT cas1_delius_booking_import_pk;
ALTER TABLE cas1_delius_booking_import ADD CONSTRAINT cas1_delius_booking_import_pk PRIMARY KEY (id);

ALTER TABLE cas1_delius_booking_import ALTER COLUMN booking_id DROP NOT NULL;
CREATE INDEX cas1_delius_booking_import_booking_id_idx ON cas1_delius_booking_import (booking_id);

ALTER TABLE cas1_delius_booking_import ADD COLUMN premises_qcode TEXT NOT NULL;
