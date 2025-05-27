CREATE TABLE cas3_premises(
    id                              UUID not null,
    name                            TEXT not null,
    postcode                        TEXT not null,
    probation_delivery_unit_id      UUID not null,
    local_authority_area_id         UUID,
    address_line1                   TEXT not null,
    address_line2                   TEXT,
    town                            TEXT,
    status                          TEXT default 'active'::TEXT not null,
    notes                           TEXT,
    created_at                      TIMESTAMP WITH TIME ZONE default now(),
    last_updated_at                 TIMESTAMP WITH TIME ZONE default now(),
    FOREIGN KEY (probation_delivery_unit_id)    REFERENCES probation_delivery_units(id),
    FOREIGN KEY (local_authority_area_id)       REFERENCES local_authority_areas(id),
    PRIMARY KEY(id)
);
