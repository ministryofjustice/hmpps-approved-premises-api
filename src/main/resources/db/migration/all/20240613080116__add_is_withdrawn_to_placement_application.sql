ALTER TABLE placement_applications
    ADD COLUMN is_withdrawn BOOLEAN NOT NULL DEFAULT FALSE;