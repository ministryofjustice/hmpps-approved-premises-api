ALTER TABLE characteristics ADD COLUMN is_active BOOL;

UPDATE characteristics
SET is_active = TRUE;

ALTER TABLE characteristics ALTER COLUMN is_active SET NOT NULL;