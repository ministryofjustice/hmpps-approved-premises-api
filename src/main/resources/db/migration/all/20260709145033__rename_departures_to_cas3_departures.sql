-- Rename the CAS3-only `departures` table to `cas3_departures` to align with the
-- CAS3 table naming convention (the table is now exclusively used by CAS3)
ALTER TABLE IF EXISTS departures RENAME TO cas3_departures;

-- Keep the supporting index name consistent with the new table name.
ALTER INDEX IF EXISTS idx_departures_booking_id RENAME TO idx_cas3_departures_booking_id;

