ALTER TABLE placement_requests ADD COLUMN is_withdrawn BOOLEAN;
UPDATE placement_requests SET is_withdrawn = false;
ALTER TABLE placement_requests ALTER COLUMN is_withdrawn SET NOT NULL;
