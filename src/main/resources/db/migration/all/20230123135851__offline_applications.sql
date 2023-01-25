CREATE TABLE offline_applications (
    id UUID NOT NULL,
    crn TEXT NOT NULL,
    service TEXT NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);
