CREATE TABLE cas_2_application_json_schemas (
    json_schema_id UUID NOT NULL,
    PRIMARY KEY (json_schema_id),
    FOREIGN KEY (json_schema_id) REFERENCES json_schemas(id)
);

INSERT INTO json_schemas (id, added_at, "schema") VALUES  ('20c06750-27d3-4dc6-8ac6-948c8f0a0210', NOW(), '{
 "$schema": "https://json-schema.org/draft/2020-12/schema",
 "title": "Application Placeholder Schema",
 "description": "An application schema that requires no properties",
 "type": "object",
 "properties": {},
 "required": []
}');

INSERT INTO cas_2_application_json_schemas (json_schema_id) VALUES
    ('20c06750-27d3-4dc6-8ac6-948c8f0a0210');
