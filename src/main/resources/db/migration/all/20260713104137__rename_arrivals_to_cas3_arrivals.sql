-- Rename the CAS3-only `arrivals` table to `cas3_arrivals` to align with the
-- CAS3 table naming convention (the table is now exclusively used by CAS3).
ALTER TABLE IF EXISTS arrivals RENAME TO cas3_arrivals;

-- Keep the supporting booking index name consistent with the new table name.
ALTER INDEX IF EXISTS idx_arrivals_booking_id RENAME TO idx_cas3_arrivals_booking_id;

