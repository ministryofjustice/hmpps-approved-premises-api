CREATE TABLE cas_2_v2_application_json_schemas
(
    json_schema_id UUID NOT NULL,
    CONSTRAINT pk_cas_2_v2_application_json_schemas PRIMARY KEY (json_schema_id)
);

CREATE TABLE cas_2_v2_application_notes
(
    id                          UUID NOT NULL,
    application_id              UUID NOT NULL,
    created_at                  TIMESTAMPTZ,
    body                        TEXT,
    assessment_id               UUID,
    created_by_nomis_user_id    UUID,
    created_by_external_user_id UUID,
    CONSTRAINT pk_cas_2_v2_application_notes PRIMARY KEY (id)
);

CREATE TABLE cas_2_v2_applications
(
    id                       UUID        NOT NULL,
    crn                      TEXT        NOT NULL,
    created_by_user_id       UUID        NOT NULL,
    data                     JSON,
    document                 JSON,
    schema_version           UUID        NOT NULL,
    created_at               TIMESTAMPTZ NOT NULL,
    submitted_at             TIMESTAMPTZ,
    abandoned_at             TIMESTAMPTZ,
    assessment_id            UUID,
    noms_number              TEXT,
    referring_prison_code    TEXT,
    preferred_areas          TEXT,
    hdc_eligibility_date     date,
    conditional_release_date date,
    telephone_number         TEXT,
    CONSTRAINT pk_cas_2_v2_applications PRIMARY KEY (id)
);

CREATE TABLE cas_2_v2_assessments
(
    id                UUID NOT NULL,
    application_id    UUID NOT NULL,
    created_at        TIMESTAMPTZ,
    nacro_referral_id TEXT,
    assessor_name     TEXT,
    CONSTRAINT pk_cas_2_v2_assessments PRIMARY KEY (id)
);

CREATE TABLE cas_2_v2_status_update_details
(
    id               UUID        NOT NULL,
    status_detail_id UUID        NOT NULL,
    label            TEXT        NOT NULL,
    status_update_id UUID        NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_cas_2_v2_status_update_details PRIMARY KEY (id)
);

CREATE TABLE cas_2_v2_status_updates
(
    id             UUID        NOT NULL,
    status_id      UUID        NOT NULL,
    description    TEXT        NOT NULL,
    label          TEXT        NOT NULL,
    assessor_id    UUID        NOT NULL,
    application_id UUID        NOT NULL,
    assessment_id  UUID,
    created_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_cas_2_v2_status_updates PRIMARY KEY (id)
);

INSERT INTO json_schemas (id, added_at, schema, type)
VALUES ('be976928-d7d3-44d5-ac3e-1463698afff5',
        '2025-01-15 15:19:37.363233 +00:00',
        '{
            "$schema": "https://json-schema.org/draft/2020-12/schema",
            "title": "Application Placeholder Schema",
            "description": "An application schema that requires no properties",
            "type": "object",
            "properties": {},
            "required": []
        }',
        'CAS_2_V2_APPLICATION');

ALTER TABLE cas_2_v2_applications
    ADD CONSTRAINT FK_CAS_2_V2_APPLICATIONS_ON_ASSESSMENT FOREIGN KEY (assessment_id) REFERENCES cas_2_v2_assessments (id);

ALTER TABLE cas_2_v2_applications
    ADD CONSTRAINT FK_CAS_2_V2_APPLICATIONS_ON_CREATED_BY_USER FOREIGN KEY (created_by_user_id) REFERENCES nomis_users (id);

ALTER TABLE cas_2_v2_applications
    ADD CONSTRAINT FK_CAS_2_V2_APPLICATIONS_ON_SCHEMA_VERSION FOREIGN KEY (schema_version) REFERENCES json_schemas (id);

ALTER TABLE cas_2_v2_application_json_schemas
    ADD CONSTRAINT FK_CAS_2_V2_APPLICATION_JSON_SCHEMAS_ON_JSON_SCHEMA FOREIGN KEY (json_schema_id) REFERENCES json_schemas (id);

ALTER TABLE cas_2_v2_application_notes
    ADD CONSTRAINT FK_CAS_2_V2_APPLICATION_NOTES_ON_APPLICATION FOREIGN KEY (application_id) REFERENCES cas_2_v2_applications (id);

ALTER TABLE cas_2_v2_application_notes
    ADD CONSTRAINT FK_CAS_2_V2_APPLICATION_NOTES_ON_ASSESSMENT FOREIGN KEY (assessment_id) REFERENCES cas_2_v2_assessments (id);

ALTER TABLE cas_2_v2_application_notes
    ADD CONSTRAINT FK_CAS_2_V2_APPLICATION_NOTES_ON_CREATED_BY_EXTERNAL_USER FOREIGN KEY (created_by_external_user_id) REFERENCES external_users (id);

ALTER TABLE cas_2_v2_application_notes
    ADD CONSTRAINT FK_CAS_2_V2_APPLICATION_NOTES_ON_CREATED_BY_NOMIS_USER FOREIGN KEY (created_by_nomis_user_id) REFERENCES nomis_users (id);

ALTER TABLE cas_2_v2_assessments
    ADD CONSTRAINT FK_CAS_2_V2_ASSESSMENTS_ON_APPLICATION FOREIGN KEY (application_id) REFERENCES cas_2_v2_applications (id);

ALTER TABLE cas_2_v2_status_updates
    ADD CONSTRAINT FK_CAS_2_V2_STATUS_UPDATES_ON_APPLICATION FOREIGN KEY (application_id) REFERENCES cas_2_v2_applications (id);

ALTER TABLE cas_2_v2_status_updates
    ADD CONSTRAINT FK_CAS_2_V2_STATUS_UPDATES_ON_ASSESSMENT FOREIGN KEY (assessment_id) REFERENCES cas_2_v2_assessments (id);

ALTER TABLE cas_2_v2_status_updates
    ADD CONSTRAINT FK_CAS_2_V2_STATUS_UPDATES_ON_ASSESSOR FOREIGN KEY (assessor_id) REFERENCES external_users (id);

ALTER TABLE cas_2_v2_status_update_details
    ADD CONSTRAINT FK_CAS_2_V2_STATUS_UPDATE_DETAILS_ON_STATUS_UPDATE FOREIGN KEY (status_update_id) REFERENCES cas_2_v2_status_updates (id);

CREATE OR REPLACE VIEW cas_2_v2_application_summary AS
SELECT a.id,
       a.crn,
       a.noms_number,
       CAST(a.created_by_user_id AS TEXT),
       nu.name,
       a.created_at,
       a.submitted_at,
       a.hdc_eligibility_date,
       asu.label,
       CAST(asu.status_id AS TEXT),
       a.referring_prison_code,
       a.conditional_release_date,
       asu.created_at AS status_created_at,
       a.abandoned_at
FROM cas_2_v2_applications a
     LEFT JOIN (SELECT DISTINCT ON (application_id) su.application_id, su.label, su.status_id, su.created_at
                FROM cas_2_v2_status_updates su
                ORDER BY su.application_id, su.created_at DESC) as asu
               ON a.id = asu.application_id
     JOIN nomis_users nu ON nu.id = a.created_by_user_id;

CREATE OR REPLACE VIEW cas_2_v2_application_live_summary AS
SELECT a.id,
       a.crn,
       a.noms_number,
       a.created_by_user_id,
       a.name,
       a.created_at,
       a.submitted_at,
       a.hdc_eligibility_date,
       a.label,
       a.status_id,
       a.referring_prison_code,
       a.abandoned_at
FROM cas_2_v2_application_summary a
WHERE (a.conditional_release_date IS NULL OR a.conditional_release_date >= current_date)
    AND a.abandoned_at IS NULL
    AND a.status_id IS NULL
   OR (a.status_id = '004e2419-9614-4c1e-a207-a8418009f23d' AND
       a.status_created_at > (current_date - INTERVAL '32 DAY')) -- Referral withdrawn
   OR (a.status_id = 'f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9' AND
       a.status_created_at > (current_date - INTERVAL '32 DAY')) -- Referral cancelled
   OR (a.status_id = '89458555-3219-44a2-9584-c4f715d6b565' AND
       a.status_created_at > (current_date - INTERVAL '32 DAY')) -- Awaiting arrival
   OR (a.status_id NOT IN ('004e2419-9614-4c1e-a207-a8418009f23d',
                           'f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9',
                           '89458555-3219-44a2-9584-c4f715d6b565'));
