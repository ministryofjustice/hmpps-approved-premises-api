CREATE TABLE approved_premises_assessments (
    assessment_id UUID NOT NULL,
    PRIMARY KEY (assessment_id),
    FOREIGN KEY (assessment_id) REFERENCES assessments(id)
);

CREATE TABLE temporary_accommodation_assessments (
    assessment_id UUID NOT NULL,
    PRIMARY KEY (assessment_id),
    FOREIGN KEY (assessment_id) REFERENCES assessments(id)
);

ALTER TABLE assessments ADD COLUMN service TEXT;

UPDATE assessments SET service = 'approved-premises';

ALTER TABLE assessments ALTER COLUMN service SET NOT NULL;

INSERT INTO approved_premises_assessments(assessment_id)
SELECT id
FROM assessments;
