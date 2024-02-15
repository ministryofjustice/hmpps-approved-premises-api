BEGIN TRANSACTION;
-- add external users to notes table
ALTER TABLE cas_2_application_notes
    ADD COLUMN created_by_external_user_id UUID;
-- add fk for external users
ALTER TABLE cas_2_application_notes
    ADD CONSTRAINT fk_created_by_external_user_id
        FOREIGN KEY(created_by_external_user_id)
        	REFERENCES external_users(id);

-- make nomis users nullable
ALTER TABLE cas_2_application_notes
    ALTER COLUMN created_by_nomis_user_id DROP NOT NULL;

-- must have either nomis or external user
ALTER TABLE cas_2_application_notes
    ADD CONSTRAINT has_user
        CHECK
        ((created_by_nomis_user_id is not null and created_by_external_user_id is null)
            OR
        (created_by_nomis_user_id is null and created_by_external_user_id is not null));

COMMIT TRANSACTION;