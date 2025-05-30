CREATE TABLE cas3_bedspaces(
    id                              UUID not null,
    premises_id                     UUID not null,
    name                            TEXT not null,
    start_date                      DATE,
    end_date                        DATE,
    notes                           TEXT,
    created_at                      TIMESTAMP WITH TIME ZONE default now(),
    last_updated_at                 TIMESTAMP WITH TIME ZONE default now(),
    FOREIGN KEY (premises_id)       REFERENCES cas3_premises(id),
    PRIMARY KEY(id)
);

CREATE INDEX cas3_bedspaces_premises_id_idx ON cas3_bedspaces (premises_id);