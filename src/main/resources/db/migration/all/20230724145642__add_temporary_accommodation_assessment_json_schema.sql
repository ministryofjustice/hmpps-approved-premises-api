CREATE TABLE temporary_accommodation_assessment_json_schemas (
    json_schema_id UUID NOT NULL,
    PRIMARY KEY (json_schema_id),
    FOREIGN KEY (json_schema_id) REFERENCES json_schemas(id)
);

INSERT INTO json_schemas (id, added_at, type, "schema") VALUES  ('325c171e-5012-43fe-840a-17fd4d7e95e6', NOW(), 'TEMPORARY_ACCOMMODATION_ASSESSMENT', '{
 "$schema": "https://json-schema.org/draft/2020-12/schema",
 "title": "Assessment Placeholder Schema",
 "description": "An assessment schema that requires no properties",
 "type": "object",
 "properties": {},
 "required": []
}');

INSERT INTO temporary_accommodation_assessment_json_schemas (json_schema_id) VALUES
    ('325c171e-5012-43fe-840a-17fd4d7e95e6');
