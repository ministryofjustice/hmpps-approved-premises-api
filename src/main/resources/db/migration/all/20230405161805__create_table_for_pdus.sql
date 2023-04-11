CREATE TABLE probation_delivery_units (
    id UUID NOT NULL,
    name TEXT NOT NULL,
    probation_region_id UUID NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (name),
    FOREIGN KEY (probation_region_id) REFERENCES probation_regions(id)
);
