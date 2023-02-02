CREATE TABLE domain_events (
    id UUID NOT NULL,
    application_id UUID NOT NULL,
    crn TEXT NOT NULL,
    type TEXT NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    data JSON NOT NULL,
    PRIMARY KEY (id)
);
