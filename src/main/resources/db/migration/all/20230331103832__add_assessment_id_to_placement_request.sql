DELETE FROM placement_requests;
ALTER TABLE placement_requests ADD COLUMN assessment_id UUID NOT NULL;
ALTER TABLE placement_requests ADD FOREIGN KEY (assessment_id) REFERENCES assessments (id);
