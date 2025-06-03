CREATE TABLE cas3_premises_characteristics(
    id              UUID not null,
    name            TEXT not null,
    property_name   TEXT not null,
    is_active       BOOLEAN not null,
    PRIMARY KEY(id)
);

CREATE TABLE cas3_premises_characteristic_mappings(
    premises_id                     UUID not null,
    premises_characteristics_id     UUID not null,
    FOREIGN KEY (premises_id) REFERENCES cas3_premises(id),
    FOREIGN KEY (premises_characteristics_id) REFERENCES cas3_premises_characteristics(id),
    PRIMARY KEY(premises_id, premises_characteristics_id)
);

CREATE INDEX cas3_pcm_premises_id_idx ON cas3_premises_characteristic_mappings(premises_id);
CREATE INDEX cas3_pcm_premises_characteristics_id_idx ON cas3_premises_characteristic_mappings(premises_characteristics_id);
