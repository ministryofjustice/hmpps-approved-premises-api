-- Create the new cas2 user table
CREATE TABLE cas_2_users
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
    CONSTRAINT pk_cas_2_users PRIMARY KEY (id)
);



-- Add the new cas2 user to the existing cas2 applications table
ALTER TABLE cas_2_applications ADD created_by_cas2_user_id UUID;



-- Add the FK constraints to the applications table
ALTER TABLE cas_2_applications
    ADD CONSTRAINT FK_CAS_2_APPLICATIONS_ON_CREATED_BY_USER FOREIGN KEY (created_by_cas2_user_id) REFERENCES cas_2_users (id);
