-- Temporary-accommodation has migrated off the shared premises table (it now uses cas3_premises).
-- Remove the leftover temporary-accommodation rows before premises is renamed to cas1_premises (CAS1-only).
-- Remove dependent premises_characteristics links first to satisfy the remaining foreign key.
DELETE FROM premises_characteristics
WHERE premises_id IN (
    SELECT id FROM premises WHERE service = 'temporary-accommodation'
);

DELETE FROM premises WHERE service = 'temporary-accommodation';