-- Rename the CAS3-only `cancellations` table to `cas3_cancellations` to align with the
-- CAS3 table naming convention (the table is now exclusively used by CAS3)
ALTER TABLE IF EXISTS cancellations RENAME TO cas3_cancellations;

-- Align the index/primary-key names on the (now renamed) cas3_cancellations table
ALTER INDEX IF EXISTS idx_cancellations_booking_id RENAME TO idx_cas3_cancellations_booking_id;
ALTER INDEX IF EXISTS cancellations_booking_id_idx RENAME TO cas3_cancellations_booking_id_idx;
ALTER INDEX IF EXISTS cancellation_pkey RENAME TO cas3_cancellations_pkey;

