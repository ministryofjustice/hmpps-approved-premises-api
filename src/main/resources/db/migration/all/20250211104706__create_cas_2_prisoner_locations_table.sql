CREATE TABLE cas_2_prisoner_locations
(
    id             UUID                     NOT NULL,
    application_id UUID                     NOT NULL,
    prison_code TEXT                     NOT NULL,
    staff_id    UUID,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date    TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id),
    CONSTRAINT fk_application_id
        FOREIGN KEY (application_id)
            REFERENCES cas_2_applications (id),
    CONSTRAINT fk_staff_id
        FOREIGN KEY (staff_id)
            REFERENCES nomis_users (id)
);

CREATE UNIQUE INDEX unique_application_id_with_null_end_date_idx
    ON cas_2_prisoner_locations (application_id)
    WHERE end_date IS NULL;
