ALTER TABLE cas_2_application_notes
    ADD CONSTRAINT cas_2_application_notes_created_by_cas2_user_id_fkey
        FOREIGN KEY (created_by_cas2_user_id) REFERENCES cas_2_users(id);
ALTER TABLE cas_2_application_notes ALTER COLUMN created_by_cas2_user_id SET NOT NULL;