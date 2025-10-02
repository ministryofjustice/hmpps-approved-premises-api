ALTER TABLE cas_2_application_notes DROP CONSTRAINT IF EXISTS has_user;
ALTER TABLE cas_2_status_updates ALTER COLUMN assessor_id DROP NOT NULL;