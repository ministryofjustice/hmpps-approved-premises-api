CREATE TABLE approved_premises (
    premises_id UUID NOT NULL,
    q_code TEXT,
    PRIMARY KEY (premises_id),
    FOREIGN KEY (premises_id) REFERENCES premises(id)
);

CREATE TABLE temporary_accommodation_premises (
    premises_id UUID NOT NULL,
    PRIMARY KEY (premises_id),
    FOREIGN KEY (premises_id) REFERENCES premises(id)
);
