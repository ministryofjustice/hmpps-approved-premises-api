ALTER TABLE approved_premises_application_json_schemas ADD COLUMN target_location_json_logic_rule TEXT;
UPDATE approved_premises_application_json_schemas SET target_location_json_logic_rule = '{"cat": ["", ""]}';
ALTER TABLE approved_premises_application_json_schemas ALTER COLUMN target_location_json_logic_rule SET NOT NULL;

ALTER TABLE approved_premises_application_json_schemas ADD COLUMN release_type_json_logic_rule TEXT;
UPDATE approved_premises_application_json_schemas SET release_type_json_logic_rule = '{"cat": ["", ""]}';
ALTER TABLE approved_premises_application_json_schemas ALTER COLUMN release_type_json_logic_rule SET NOT NULL;
