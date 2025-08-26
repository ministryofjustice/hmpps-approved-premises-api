ALTER TABLE cas_2_status_updates
    ADD CONSTRAINT fk_cas_2_assessor
        FOREIGN KEY(assessor_id)
            REFERENCES cas_2_users(id);

ALTER TABLE cas_2_status_updates
    DROP constraint IF EXISTS fk_assessor;

ALTER TABLE cas_2_application_notes
    DROP constraint IF EXISTS has_user;

ALTER TABLE cas_2_application_notes
    ADD CONSTRAINT has_user_cas2
        CHECK
            ((created_by_cas2_user_id is null)
            OR
             (created_by_cas2_user_id is not null));