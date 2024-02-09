-- create new table for CAS2 status updates
CREATE TABLE cas_2_application_notes (
    id                           UUID                        NOT NULL,
    application_id               UUID                        NOT NULL,
    created_by_nomis_user_id     UUID                        NOT NULL,
    body                         TEXT                        NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE                NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_application_id
        FOREIGN KEY(application_id)
    	    REFERENCES cas_2_applications(id),

    CONSTRAINT fk_created_by_nomis_user_id
        FOREIGN KEY(created_by_nomis_user_id)
    	    REFERENCES nomis_users(id)
);

-- index for foreign key to applications table
CREATE INDEX ON cas_2_application_notes(application_id);

-- index for foreign key to nomis users table
CREATE INDEX ON cas_2_application_notes(created_by_nomis_user_id);

