ALTER TABLE cas_2_application_assignments DROP COLUMN allocated_pom_user_id CASCADE;
ALTER TABLE cas_2_application_notes DROP COLUMN created_by_nomis_user_id;
ALTER TABLE cas_2_application_notes DROP COLUMN created_by_external_user_id;
ALTER TABLE cas_2_applications DROP COLUMN created_by_user_id;
ALTER TABLE cas_2_assessments DROP COLUMN application_origin;
ALTER TABLE cas_2_status_updates DROP COLUMN assessor_id;
ALTER TABLE cas_2_users DROP COLUMN updated_at;
