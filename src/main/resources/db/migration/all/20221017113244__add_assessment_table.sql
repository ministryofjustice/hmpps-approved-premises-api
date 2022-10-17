INSERT INTO json_schemas (id, added_at, "schema", type) VALUES
    ('8ffd9c83-5b3a-456b-8834-47ca2a9f6cbb', NOW(), '{
 "$schema": "https://json-schema.org/draft/2020-12/schema",
 "title": "Assessment Placeholder Schema",
 "description": "An assessment schema that requires no properties",
 "type": "object",
 "properties": {},
 "required": []
}', 'ASSESSMENT');

CREATE TABLE assessments (
    id UUID NOT NULL,
    application_id UUID NOT NULL,
    data JSON,
    document JSON,
    schema_version UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    allocated_to_probation_officer_id UUID NOT NULL,
    allocated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id),
    FOREIGN KEY (allocated_to_probation_officer_id) REFERENCES probation_officers(id),
    FOREIGN KEY (schema_version) REFERENCES json_schemas(id)
);
