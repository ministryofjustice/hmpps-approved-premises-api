-- Rename the CAS3-only `non_arrivals` table to `cas3_non_arrivals` to align with the
-- CAS3 table naming convention (the table is now exclusively used by CAS3)
ALTER TABLE IF EXISTS non_arrivals RENAME TO cas3_non_arrivals;

-- Keep the supporting index names consistent with the new table name.
ALTER INDEX IF EXISTS non_arrival_pkey RENAME TO cas3_non_arrivals_pkey;
ALTER INDEX IF EXISTS idx_non_arrivals_booking_id RENAME TO idx_cas3_non_arrivals_booking_id;