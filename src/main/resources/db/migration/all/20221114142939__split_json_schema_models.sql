CREATE TABLE approved_premises_application_json_schemas (
    json_schema_id UUID NOT NULL,
    is_womens_json_logic_rule TEXT NOT NULL,
    is_pipe_json_logic_rule TEXT NOT NULL,
    PRIMARY KEY (json_schema_id),
    FOREIGN KEY (json_schema_id) REFERENCES json_schemas(id)
);

INSERT INTO approved_premises_application_json_schemas (json_schema_id, is_womens_json_logic_rule, is_pipe_json_logic_rule) VALUES
    ('f96725f6-27ac-46f2-83e0-00cf4af48370', '{"==": [1, 2]}', '{"==", [1, 1]}');

CREATE TABLE approved_premises_assessment_json_schemas (
    json_schema_id UUID NOT NULL,
    PRIMARY KEY (json_schema_id),
    FOREIGN KEY (json_schema_id) REFERENCES json_schemas(id)
);
