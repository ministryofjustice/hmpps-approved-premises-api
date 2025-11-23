ALTER TABLE cas_2_application_assignments
ADD CONSTRAINT cas_2_application_assignments_allocated_pom_user_id_cas_2_fkey
        FOREIGN KEY (allocated_pom_cas_2_user_id) REFERENCES cas_2_users(id);