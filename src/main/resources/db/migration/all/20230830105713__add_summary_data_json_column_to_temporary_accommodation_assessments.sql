ALTER TABLE temporary_accommodation_assessments ADD COLUMN summary_data JSON;

UPDATE temporary_accommodation_assessments
SET summary_data = '{}';

ALTER TABLE temporary_accommodation_assessments ALTER COLUMN summary_data SET NOT NULL;
