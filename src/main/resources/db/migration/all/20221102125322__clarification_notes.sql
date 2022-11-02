CREATE TABLE assessment_clarification_notes (
    id UUID NOT NULL,
    assessment_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    text TEXT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (assessment_id) REFERENCES assessments(id),
    FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);