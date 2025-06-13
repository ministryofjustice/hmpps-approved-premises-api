
-- Add the new cas2 user to the existing cas2 application note entity table
ALTER TABLE cas_2_application_notes ADD created_by_cas2_user_id UUID;



-- Add the FK constraints to the cas2 application note entity table
ALTER TABLE cas_2_application_notes
    ADD CONSTRAINT FK_CAS_2_APPLICATIONS_NOTES_ON_CREATED_BY_USER
        FOREIGN KEY (created_by_cas2_user_id) REFERENCES cas_2_users (id);
