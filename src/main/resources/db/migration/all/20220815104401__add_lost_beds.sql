CREATE TABLE lost_beds (
    id UUID NOT NULL,
    premises_id UUID NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    number_of_beds INT NOT NULL,
    reason TEXT NOT NULL,
    reference_number TEXT,
    notes TEXT,
    PRIMARY KEY (id),
    FOREIGN KEY (premises_id) REFERENCES premises(id)
);
