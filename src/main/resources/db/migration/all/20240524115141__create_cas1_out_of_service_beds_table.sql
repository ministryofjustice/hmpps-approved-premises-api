CREATE TABLE cas1_out_of_service_bed_reasons (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    name TEXT NOT NULL,
    is_active BOOL NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (name)
);

CREATE TABLE cas1_out_of_service_beds (
    id UUID NOT NULL,
    premises_id UUID NOT NULL,
    out_of_service_bed_reason_id UUID NOT NULL,
    bed_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reference_number TEXT NULL,
    notes TEXT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (premises_id) REFERENCES approved_premises(premises_id),
    FOREIGN KEY (out_of_service_bed_reason_id) REFERENCES cas1_out_of_service_bed_reasons(id),
    FOREIGN KEY (bed_id) REFERENCES beds(id)
);
