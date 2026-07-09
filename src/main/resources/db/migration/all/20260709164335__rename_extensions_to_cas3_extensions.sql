-- Rename the CAS3-only `extensions` table to `cas3_extensions` to align with the
-- CAS3 table naming convention (the table is now exclusively used by CAS3)
ALTER TABLE IF EXISTS extensions RENAME TO cas3_extensions;
