CREATE TABLE placement_applications (
    id uuid NOT NULL,
    application_id uuid NOT NULL,
    created_by_user_id uuid NOT NULL,
    data json NULL,
    document json NULL,
    schema_version uuid NULL,
    created_at timestamp NOT NULL,
    submitted_at timestamp NULL,
    allocated_to_user_id uuid NULL,
    allocated_at timestamp NULL,
    reallocated_at timestamp NULL,
    decision TEXT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (application_id) REFERENCES applications(id),
    FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    FOREIGN KEY (allocated_to_user_id) REFERENCES users(id),
    FOREIGN KEY (schema_version) REFERENCES json_schemas(id)
);

CREATE TABLE approved_premises_placement_application_json_schemas (
    json_schema_id UUID NOT NULL,
    PRIMARY KEY (json_schema_id),
    FOREIGN KEY (json_schema_id) REFERENCES json_schemas(id)
);