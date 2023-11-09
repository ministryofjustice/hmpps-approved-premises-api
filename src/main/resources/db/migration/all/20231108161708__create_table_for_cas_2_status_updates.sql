-- create new table for CAS2 status updates
CREATE TABLE cas_2_status_updates (
    id              UUID                        NOT NULL,
    status_id       UUID                        NOT NULL,
    application_id  UUID                        NOT NULL,
    assessor_id     UUID                        NOT NULL,
    description     TEXT                        NOT NULL,
    label           TEXT                        NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE    NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_application
        FOREIGN KEY(application_id)
    	    REFERENCES cas_2_applications(id),

    CONSTRAINT fk_assessor
        FOREIGN KEY(assessor_id)
            REFERENCES external_users(id)

);
-- index for foreign key to cas_2_applications table
CREATE INDEX ON cas_2_status_updates(application_id);

-- index for foreign key to external_users table
CREATE INDEX ON cas_2_status_updates(assessor_id);
