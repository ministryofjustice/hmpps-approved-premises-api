CREATE TABLE postcode_districts (
    id UUID NOT NULL,
    outcode TEXT NOT NULL,
    latitude DECIMAL NOT NULL,
    longitude DECIMAL NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (outcode)
);
