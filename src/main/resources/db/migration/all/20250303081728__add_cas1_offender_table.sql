CREATE TABLE cas1_offenders (
    id UUID NOT NULL ,
    crn TEXT NOT NULL ,
    noms_number TEXT,
    tier TEXT,
    name TEXT NOT NULL ,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);
