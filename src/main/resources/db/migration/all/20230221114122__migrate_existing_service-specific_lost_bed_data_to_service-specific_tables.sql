INSERT INTO approved_premises_lost_beds (lost_bed_id, number_of_beds)
SELECT id, number_of_beds
FROM lost_beds;

ALTER TABLE lost_beds ADD COLUMN service TEXT;

UPDATE lost_beds
SET service = 'approved-premises';

ALTER TABLE lost_beds ALTER COLUMN service SET NOT NULL;
