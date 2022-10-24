CREATE TABLE characteristic (
    id UUID NOT NULL,
    name TEXT NOT NULL,
    service_scope TEXT NOT NULL DEFAULT '*',
    model_scope TEXT NOT NULL DEFAULT '*',
    PRIMARY KEY (id)
);
