ALTER TABLE applications DROP CONSTRAINT applications_schema_version_fkey;
ALTER TABLE approved_premises_application_json_schemas DROP CONSTRAINT approved_premises_application_json_schemas_json_schema_id_fkey;
ALTER TABLE temporary_accommodation_application_json_schemas DROP CONSTRAINT temporary_accommodation_application_json_sc_json_schema_id_fkey;

ALTER TABLE applications DROP COLUMN schema_version;

DROP TABLE approved_premises_application_json_schemas;
DROP TABLE temporary_accommodation_application_json_schemas;