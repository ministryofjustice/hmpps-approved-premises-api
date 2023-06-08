INSERT INTO json_schemas (id, added_at, "schema", type) VALUES
    ('c6ffcbb0-3b1b-4336-ae57-ff80dd41f7e0', NOW(), '{
 "$schema": "https://json-schema.org/draft/2020-12/schema",
 "title": "Placement Application Placeholder Schema",
 "description": "An placement application schema that requires no properties",
 "type": "object",
 "properties": {},
 "required": []
}', 'APPROVED_PREMISES_PLACEMENT_APPLICATION');

INSERT into approved_premises_placement_application_json_schemas (json_schema_id) VALUES ('c6ffcbb0-3b1b-4336-ae57-ff80dd41f7e0');