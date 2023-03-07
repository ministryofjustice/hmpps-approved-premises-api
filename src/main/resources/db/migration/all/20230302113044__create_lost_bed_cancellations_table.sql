CREATE TABLE lost_bed_cancellations(
    id UUID NOT NULL,
    lost_bed_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    notes TEXT,
    PRIMARY KEY (id),
    FOREIGN KEY (lost_bed_id) REFERENCES lost_beds(id)
);
