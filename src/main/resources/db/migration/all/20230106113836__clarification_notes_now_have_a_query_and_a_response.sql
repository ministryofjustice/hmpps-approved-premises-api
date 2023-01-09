ALTER TABLE assessment_clarification_notes
    RENAME COLUMN text TO query;

ALTER TABLE assessment_clarification_notes ADD COLUMN response text;
