ALTER TABLE placement_requests ADD COLUMN is_parole BOOLEAN;
UPDATE placement_requests SET is_parole = FALSE;
ALTER TABLE placement_requests ALTER COLUMN is_parole SET NOT NULL;