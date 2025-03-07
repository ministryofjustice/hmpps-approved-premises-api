DROP TABLE cas_2_prisoner_locations;

CREATE TABLE cas_2_application_assignments
(
    id                    UUID                     NOT NULL,
    application_id        UUID                     NOT NULL,
    prison_code           TEXT                     NOT NULL,
    allocated_pom_user_id UUID NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (application_id) REFERENCES cas_2_applications (id),
    FOREIGN KEY (allocated_pom_user_id) REFERENCES nomis_users (id)
);

INSERT INTO cas_2_application_assignments (id, application_id, prison_code, allocated_pom_user_id, created_at)
    (SELECT gen_random_uuid(),
            app.id,
            app.referring_prison_code,
            app.created_by_user_id,
            app.submitted_at
     FROM cas_2_applications app
     WHERE submitted_at IS NOT NULL
     ORDER BY submitted_at DESC
     LIMIT 1)
;