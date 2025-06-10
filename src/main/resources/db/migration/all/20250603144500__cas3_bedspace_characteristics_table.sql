CREATE TABLE cas3_bedspace_characteristics(
    id              UUID not null,
    description     TEXT not null,
    name            TEXT,
    is_active       BOOLEAN not null,
    PRIMARY KEY(id)
);

CREATE TABLE cas3_bedspace_characteristic_assignments(
    bedspace_id                     UUID not null,
    bedspace_characteristics_id     UUID not null,
    FOREIGN KEY (bedspace_id) REFERENCES cas3_bedspaces(id),
    FOREIGN KEY (bedspace_characteristics_id) REFERENCES cas3_bedspace_characteristics(id),
    PRIMARY KEY(bedspace_id, bedspace_characteristics_id)
);
