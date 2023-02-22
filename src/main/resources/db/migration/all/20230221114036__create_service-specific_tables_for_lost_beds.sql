CREATE TABLE approved_premises_lost_beds (
    lost_bed_id UUID NOT NULL,
    number_of_beds INT NOT NULL,
    PRIMARY KEY (lost_bed_id),
    FOREIGN KEY (lost_bed_id) REFERENCES lost_beds(id)
);

CREATE TABLE temporary_accommodation_lost_beds (
    lost_bed_id UUID NOT NULL,
    bed_id UUID NOT NULL,
    PRIMARY KEY (lost_bed_id),
    FOREIGN KEY (lost_bed_id) REFERENCES lost_beds(id),
    FOREIGN KEY (bed_id) REFERENCES beds(id)
);
