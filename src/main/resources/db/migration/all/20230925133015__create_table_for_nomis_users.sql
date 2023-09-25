-- create new table for nomis users
CREATE TABLE nomis_users (
    id              UUID                        NOT NULL,
    nomis_username  TEXT                        NOT NULL,
    nomis_staff_id  BIGINT                      NOT NULL,
    name            TEXT                        NOT NULL,
    account_type    TEXT                        NOT NULL,
    is_enabled      BOOLEAN                     NOT NULL,
    is_active       BOOLEAN                     NOT NULL,
    email           TEXT                        DEFAULT 'unknown@digital.justice.gov.uk',
    created_at      TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE,

    PRIMARY KEY (id),
    UNIQUE (nomis_username)
);

-- truncate pre-existing cas2 applications
--   Run this command manually in a Postgres SQL console if necessary,
--   but only in a non-production environment!
--   TRUNCATE TABLE cas_2_applications CASCADE;

-- alter foreign key constraint for cas_2_applications <-> nomis_users rather than users table
ALTER TABLE cas_2_applications DROP CONSTRAINT cas2_apps_user_id_fk;
ALTER TABLE cas_2_applications ADD CONSTRAINT cas2_apps_nomis_user_id_fk FOREIGN KEY (created_by_user_id) REFERENCES nomis_users(id);
