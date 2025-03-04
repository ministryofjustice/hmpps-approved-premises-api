--create the new table
CREATE TABLE cas_2_offenders
(
    id          uuid                     NOT NULL,
    noms_number text                     NOT NULL,
    crn         text                     NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (noms_number),
    UNIQUE (crn)
);
CREATE INDEX cas_2_offenders_noms_number_idx ON cas_2_offenders (noms_number);
CREATE INDEX cas_2_offenders_crn_idx ON cas_2_offenders (crn);

--back fill it with offender information from previous submitted applications
INSERT INTO cas_2_offenders (id, noms_number, crn, created_at, updated_at)
    (SELECT gen_random_uuid(),
            app.noms_number,
            app.crn,
            app.submitted_at,
            app.submitted_at
     FROM cas_2_applications app
     WHERE submitted_at IS NOT NULL);

--clear the old data from prisoner locations (not used and will be re-back filled).
DELETE
FROM cas_2_prisoner_locations;

-- make changes to the prisoner locations table
ALTER TABLE cas_2_prisoner_locations
    DROP CONSTRAINT IF EXISTS fk_staff_id;
ALTER TABLE cas_2_prisoner_locations
    DROP COLUMN IF EXISTS application_id;
ALTER TABLE cas_2_prisoner_locations
    DROP COLUMN IF EXISTS end_date;
DROP INDEX IF EXISTS unique_application_id_with_null_end_date_idx;

ALTER TABLE cas_2_prisoner_locations
    RENAME COLUMN occurred_at TO created_at;
ALTER TABLE cas_2_prisoner_locations
    RENAME COLUMN staff_id TO allocated_pom_user_id;

ALTER TABLE cas_2_prisoner_locations
    ADD COLUMN offender_id UUID NULL;

ALTER TABLE cas_2_prisoner_locations
    ADD CONSTRAINT fk_cas2_prisoner_locations_offender_id
        FOREIGN KEY (offender_id) REFERENCES cas_2_offenders (id);
ALTER TABLE cas_2_prisoner_locations
    ADD CONSTRAINT fk_cas2_prisoner_locations_allocated_pom_user_id
        FOREIGN KEY (allocated_pom_user_id) REFERENCES nomis_users (id);

INSERT INTO cas_2_prisoner_locations (id, offender_id, prison_code, allocated_pom_user_id, created_at)
    (SELECT gen_random_uuid()
          , (SELECT id
             FROM cas_2_offenders
             WHERE noms_number = app.noms_number)
          , app.referring_prison_code
          , app.created_by_user_id
          , app.submitted_at
     FROM cas_2_applications app
     WHERE submitted_at iS NOT NULL);

ALTER TABLE cas_2_prisoner_locations
    ALTER COLUMN offender_id
        SET NOT NULL