CREATE TABLE cas3_premises_characteristics(
    id              UUID not null,
    description      TEXT not null,
    name            TEXT,
    is_active       BOOLEAN not null,
    PRIMARY KEY(id)
);

CREATE TABLE cas3_premises_characteristic_assignments(
    premises_id                     UUID not null,
    premises_characteristics_id     UUID not null,
    FOREIGN KEY (premises_id) REFERENCES cas3_premises(id),
    FOREIGN KEY (premises_characteristics_id) REFERENCES cas3_premises_characteristics(id),
    PRIMARY KEY(premises_id, premises_characteristics_id)
);
