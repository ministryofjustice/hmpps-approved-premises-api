CREATE TABLE cas1_out_of_service_bed_cancellations (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    notes TEXT NULL,
    out_of_service_bed_id UUID NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (out_of_service_bed_id) REFERENCES cas1_out_of_service_beds(id)
);
