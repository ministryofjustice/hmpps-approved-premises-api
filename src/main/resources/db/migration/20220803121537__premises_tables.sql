CREATE TABLE ap_area (
    id UUID NOT NULL,
    name TEXT NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE local_authority_area (
    id UUID NOT NULL,
    identifier TEXT NOT NULL,
    name TEXT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (identifier)
);

CREATE TABLE probation_region (
    id UUID NOT NULL,
    identifier TEXT NOT NULL,
    name TEXT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (identifier)
);

CREATE TABLE premises (
    id UUID NOT NULL,
    name TEXT NOT NULL,
    ap_code TEXT NOT NULL,
    postcode TEXT NOT NULL,
    total_beds INT NOT NULL,
    probation_region_id UUID NOT NULL,
    ap_area_id UUID NOT NULL,
    local_authority_area_id UUID NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY (probation_region_id) REFERENCES probation_region(id),
    FOREIGN KEY (ap_area_id) REFERENCES ap_area(id),
    FOREIGN KEY (probation_region_id) REFERENCES probation_region(id)
);
