CREATE TABLE application_timeline_notes (
    id UUID NOT NULL,
    application_id UUID NOT NULL,
    created_by UUID NOT NULL,
    created_at_date DATE NOT NULL,
    body TEXT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (application_id) REFERENCES approved_premises_applications(id)
);