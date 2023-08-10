CREATE TABLE assessment_referral_history_notes (
    id UUID NOT NULL,
    assessment_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    message TEXT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (assessment_id) REFERENCES assessments(id)
);

CREATE TABLE assessment_referral_history_user_notes (
    id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    FOREIGN KEY (id) REFERENCES assessment_referral_history_notes(id),
    FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);