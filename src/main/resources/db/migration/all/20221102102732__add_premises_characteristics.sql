CREATE TABLE premises_characteristics
(
    premises_id           UUID NOT NULL,
    characteristic_id UUID NOT NULL,
    PRIMARY KEY (premises_id, characteristic_id),
    FOREIGN KEY (premises_id) REFERENCES premises (id),
    FOREIGN KEY (characteristic_id) REFERENCES characteristics (id)
)