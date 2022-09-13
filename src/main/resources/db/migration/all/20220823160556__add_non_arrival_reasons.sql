CREATE TABLE non_arrival_reasons (
    id UUID NOT NULL,
    name TEXT NOT NULL,
    is_active BOOL NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (name)
);

ALTER TABLE non_arrivals DROP COLUMN reason;
ALTER TABLE non_arrivals ADD COLUMN non_arrival_reason_id UUID NOT NULL;
ALTER TABLE non_arrivals ADD CONSTRAINT non_arrival_reason_id_fk FOREIGN KEY (non_arrival_reason_id) REFERENCES non_arrival_reasons(id);
