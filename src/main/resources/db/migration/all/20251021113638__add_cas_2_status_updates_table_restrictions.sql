ALTER TABLE cas_2_status_updates
    ADD CONSTRAINT cas_2_status_updates_cas2_user_assessor_id_fkey
        FOREIGN KEY (cas2_user_assessor_id) REFERENCES cas_2_users(id);
ALTER TABLE cas_2_status_updates ALTER COLUMN cas2_user_assessor_id SET NOT NULL;