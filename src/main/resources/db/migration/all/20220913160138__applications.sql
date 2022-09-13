CREATE TABLE application_schemas (
    id UUID NOT NULL,
    added_at TIMESTAMP WITH TIME ZONE NOT NULL,
    schema TEXT NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE probation_officers (
    id UUID NOT NULL,
    name TEXT NOT NULL,
    distinguished_name TEXT NOT NULL,
    is_active BOOLEAN NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE applications (
    id UUID NOT NULL,
    created_by_probation_officer_id UUID NOT NULL,
    crn TEXT NOT NULL,
    data JSON,
    schema_version UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE,
    FOREIGN KEY (created_by_probation_officer_id) REFERENCES probation_officers(id),
    FOREIGN KEY (schema_version) REFERENCES application_schemas(id)
);
