-- truncate pre-existing cas2 applications
TRUNCATE TABLE cas_2_applications CASCADE;

-- remove foreign key to abstract applications table
ALTER TABLE cas_2_applications DROP constraint IF EXISTS cas_2_applications_id_fkey;

-- add fields from abstract applications table
ALTER TABLE cas_2_applications ADD COLUMN created_by_user_id UUID NOT NULL;
ALTER TABLE cas_2_applications ADD COLUMN crn                TEXT NOT NULL;
ALTER TABLE cas_2_applications ADD COLUMN data               JSON;
ALTER TABLE cas_2_applications ADD COLUMN schema_version     UUID NOT NULL;
ALTER TABLE cas_2_applications ADD COLUMN created_at         TIMESTAMP WITH TIME ZONE NOT NULL;
ALTER TABLE cas_2_applications ADD COLUMN submitted_at       TIMESTAMP WITH TIME ZONE;
ALTER TABLE cas_2_applications ADD COLUMN document           JSON;
ALTER TABLE cas_2_applications ADD COLUMN noms_number        TEXT;

-- index for foreign key to users table
CREATE INDEX ON cas_2_applications(created_by_user_id);

-- define foreign keys constraints for cas_2_applications
ALTER TABLE cas_2_applications ADD CONSTRAINT cas2_apps_user_id_fk FOREIGN KEY (created_by_user_id) REFERENCES users(id);
ALTER TABLE cas_2_applications ADD CONSTRAINT cas2_apps_schema_version_fk FOREIGN KEY (schema_version) REFERENCES json_schemas(id);
