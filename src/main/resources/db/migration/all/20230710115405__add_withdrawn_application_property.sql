ALTER TABLE approved_premises_applications ADD COLUMN is_withdrawn BOOLEAN;

UPDATE approved_premises_applications SET is_withdrawn = false;

ALTER TABLE approved_premises_applications ALTER COLUMN is_withdrawn SET NOT NULL;
