CREATE TABLE appeals (
    id UUID NOT NULL,
    appeal_date DATE NOT NULL,
    appeal_detail TEXT NOT NULL,
    reviewer TEXT NOT NULL,
    decision TEXT NOT NULL,
    decision_detail TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    application_id UUID NOT NULL,
    assessment_id UUID,
    created_by_user_id UUID NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (application_id) REFERENCES applications(id),
    FOREIGN KEY (assessment_id) REFERENCES assessments(id),
    FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);
