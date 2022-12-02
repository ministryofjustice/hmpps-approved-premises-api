UPDATE approved_premises_application_json_schemas
SET
  is_pipe_json_logic_rule = '{"==": [1, 0]}'
WHERE
  json_schema_id = 'f96725f6-27ac-46f2-83e0-00cf4af48370';