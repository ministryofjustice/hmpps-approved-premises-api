ALTER TABLE cas_2_users ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE cas_2_users ADD COLUMN external_type TEXT;
ALTER TABLE cas_2_users ADD COLUMN nomis_account_type TEXT;

ALTER TABLE cas_2_assessments ADD application_origin text NOT NULL DEFAULT 'homeDetentionCurfew';

ALTER TABLE cas_2_status_updates ADD COLUMN cas2_user_assessor_id UUID;

ALTER TABLE cas_2_applications ALTER COLUMN created_by_user_id DROP NOT NULL;

ALTER TABLE cas_2_application_assignments ADD COLUMN allocated_pom_cas_2_user_id UUID;
ALTER TABLE cas_2_application_assignments
    ADD CONSTRAINT cas_2_application_assignments_allocated_pom_user_id_cas_2_fkey
        FOREIGN KEY (allocated_pom_cas_2_user_id) REFERENCES cas_2_users(id);