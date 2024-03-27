-- add a nullable foreign key for assessments
ALTER TABLE cas_2_status_updates
ADD assessment_id UUID,
ADD FOREIGN KEY(assessment_id)
    REFERENCES cas_2_assessments(id),
ALTER COLUMN application_id
    DROP NOT NULL;

-- index on the assessment id
CREATE INDEX ON cas_2_status_updates(assessment_id);

