CREATE TABLE cas3_bedspace_characteristics(
    id              UUID not null,
    name            TEXT not null,
    property_name   TEXT not null,
    is_active       BOOLEAN not null,
    PRIMARY KEY(id)
);

CREATE TABLE cas3_bedspace_characteristic_mappings(
    bedspace_id                     UUID not null,
    bedspace_characteristics_id     UUID not null,
    FOREIGN KEY (bedspace_id) REFERENCES cas3_bedspaces(id),
    FOREIGN KEY (bedspace_characteristics_id) REFERENCES cas3_bedspace_characteristics(id),
    PRIMARY KEY(bedspace_id, bedspace_characteristics_id)
);

CREATE INDEX cas3_bcm_bedspace_id_idx ON cas3_bedspace_characteristic_mappings(bedspace_id);
CREATE INDEX cas3_bcm_bedspace_characteristics_id_idx ON cas3_bedspace_characteristic_mappings(bedspace_characteristics_id);
