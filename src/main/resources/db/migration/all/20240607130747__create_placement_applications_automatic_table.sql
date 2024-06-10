CREATE TABLE placement_applications_automatic (
    id UUID NOT NULL,
    application_id UUID NOT NULL,
    submitted_at timestamp NOT NULL,
    expected_arrival_date timestamp NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_application_id
        FOREIGN KEY(application_id)
            REFERENCES applications(id)
);

CREATE INDEX ON placement_applications_automatic(application_id);