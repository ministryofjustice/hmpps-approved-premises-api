-- create new table for CAS2 Assessments
CREATE TABLE cas_2_assessments (
    id                           UUID                        NOT NULL,
    application_id               UUID                        NOT NULL,
    nacro_referral_id            TEXT                              ,
    assessor_name                TEXT                                ,
    created_at       TIMESTAMP WITH TIME ZONE                NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_application_id
        FOREIGN KEY(application_id)
    	    REFERENCES cas_2_applications(id)
);

-- index for foreign key to applications table
CREATE INDEX ON cas_2_assessments(application_id);
