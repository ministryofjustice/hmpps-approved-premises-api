CREATE INDEX IF NOT EXISTS cas_2_applications_submitted_at_created_by_user_idx
    ON cas_2_applications (submitted_at, created_by_user_id);

CREATE INDEX IF NOT EXISTS cas_2_status_updates_application_id_created_at_idx
    ON cas_2_status_updates (application_id, created_at DESC);

CREATE INDEX IF NOT EXISTS cas_2_application_assignments_application_id_created_at_idx
    ON cas_2_application_assignments (application_id, created_at DESC);

CREATE INDEX IF NOT EXISTS cas_2_application_assignments_application_id_idx
    ON cas_2_application_assignments (application_id);

CREATE INDEX IF NOT EXISTS cas_2_application_assignments_allocated_pom_user_idx
    ON cas_2_application_assignments (allocated_pom_user_id);

CREATE INDEX IF NOT EXISTS cas_2_application_assignments_prison_code_idx
    ON cas_2_application_assignments (prison_code);