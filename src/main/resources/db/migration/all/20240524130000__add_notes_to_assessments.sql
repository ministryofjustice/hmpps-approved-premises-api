BEGIN TRANSACTION;
-- add a nullable foreign key for assessments
ALTER TABLE cas_2_application_notes
    ADD COLUMN assessment_id UUID;

ALTER TABLE cas_2_application_notes
    ADD FOREIGN KEY (assessment_id) REFERENCES cas_2_assessments (id);

-- index on the assessment id
CREATE INDEX ON cas_2_application_notes(assessment_id);
COMMIT;