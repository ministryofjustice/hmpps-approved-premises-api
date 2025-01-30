
CREATE TABLE cas_2_v2_users
(
    id                       UUID    NOT NULL,
    name                     TEXT    NOT NULL,
    username                 TEXT    NOT NULL,
    email                    TEXT,
    user_type                TEXT    NOT NULL,
    nomis_staff_id           BIGINT,
    active_nomis_caseload_id TEXT,
    delius_staff_code        TEXT,
    delius_team_codes        TEXT,
    is_enabled               BOOLEAN NOT NULL,
    is_active                BOOLEAN NOT NULL,
    created_at               TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_cas_2_v2_users PRIMARY KEY (id)
);

ALTER TABLE cas_2_v2_applications DROP CONSTRAINT FK_CAS_2_V2_APPLICATIONS_ON_CREATED_BY_USER;
ALTER TABLE cas_2_v2_applications
    ADD CONSTRAINT FK_CAS_2_V2_APPLICATIONS_ON_CREATED_BY_USER FOREIGN KEY (created_by_user_id) REFERENCES cas_2_v2_users (id);

ALTER TABLE cas_2_v2_application_notes DROP CONSTRAINT FK_CAS_2_V2_APPLICATION_NOTES_ON_CREATED_BY_EXTERNAL_USER;
ALTER TABLE cas_2_v2_application_notes DROP CONSTRAINT FK_CAS_2_V2_APPLICATION_NOTES_ON_CREATED_BY_NOMIS_USER;
ALTER TABLE cas_2_v2_application_notes DROP COLUMN created_by_nomis_user_id;
ALTER TABLE cas_2_v2_application_notes DROP COLUMN created_by_external_user_id;
ALTER TABLE cas_2_v2_application_notes ADD COLUMN created_by_user_id UUID NOT NULL;
ALTER TABLE cas_2_v2_application_notes  ADD CONSTRAINT FK_CAS_2_V2_APPLICATION_NOTES_ON_CREATED_BY_USER FOREIGN KEY (created_by_user_id) REFERENCES cas_2_v2_users (id);

ALTER TABLE cas_2_v2_status_updates DROP CONSTRAINT FK_CAS_2_V2_STATUS_UPDATES_ON_ASSESSOR;
ALTER TABLE cas_2_v2_status_updates
    ADD CONSTRAINT FK_CAS_2_V2_STATUS_UPDATES_ON_ASSESSOR FOREIGN KEY (assessor_id) REFERENCES cas_2_v2_users (id);

CREATE OR REPLACE VIEW cas_2_v2_application_summary AS
SELECT a.id,
       a.crn,
       a.noms_number,
       CAST(a.created_by_user_id AS TEXT),
       u.name,
       a.created_at,
       a.submitted_at,
       a.hdc_eligibility_date,
       asu.label,
       CAST(asu.status_id AS TEXT),
       a.referring_prison_code,
       a.conditional_release_date,
       asu.created_at AS status_created_at,
       a.abandoned_at,
       a.application_origin,
       a.bail_hearing_date
FROM cas_2_v2_applications a
     LEFT JOIN (SELECT DISTINCT ON (application_id) su.application_id, su.label, su.status_id, su.created_at
                FROM cas_2_v2_status_updates su
                ORDER BY su.application_id, su.created_at DESC) as asu
               ON a.id = asu.application_id
     JOIN cas_2_v2_users u ON u.id = a.created_by_user_id;