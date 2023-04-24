CREATE TABLE temporary_accommodation_application_json_schemas (
    json_schema_id UUID NOT NULL,
    PRIMARY KEY (json_schema_id),
    FOREIGN KEY (json_schema_id) REFERENCES json_schemas(id)
);

INSERT INTO json_schemas (id, added_at, "schema") VALUES  ('18499823-422e-4813-be61-ef24d2534a18', NOW(), '{
 "$schema": "https://json-schema.org/draft/2020-12/schema",
 "title": "Application Placeholder Schema",
 "description": "An application schema that requires no properties",
 "type": "object",
 "properties": {},
 "required": []
}');

INSERT INTO temporary_accommodation_application_json_schemas (json_schema_id) VALUES
    ('18499823-422e-4813-be61-ef24d2534a18');
