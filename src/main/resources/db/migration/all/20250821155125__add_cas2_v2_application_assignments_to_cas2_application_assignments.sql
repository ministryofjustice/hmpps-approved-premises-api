ALTER TABLE cas_2_application_assignments
    ADD CONSTRAINT cas_2_application_assignments_allocated_pom_user_id_cas_2_fkey
        FOREIGN KEY (allocated_pom_user_id) REFERENCES cas_2_users(id);

ALTER TABLE cas_2_application_assignments DROP constraint IF EXISTS cas_2_application_assignments_allocated_pom_user_id_fkey;

INSERT INTO cas_2_application_assignments as aa
SELECT
    -- TODO besscerule - i've reused the app_id as the identifier id - would it be better to create a new one and would that cause duplication?
    a.id as id,
    a.id as application_id,
    a.referring_prison_code as prison_code,
    a.created_by_user_id as allocated_pom_user_id,
    now() as created_at
FROM cas_2_applications as a;