CREATE TABLE cas1_out_of_service_bed_revisions (
    id UUID NOT NULL,
    out_of_service_bed_id UUID NOT NULL,
    out_of_service_bed_reason_id UUID NOT NULL,
    created_by_user_id UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revision_type TEXT NOT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    reference_number TEXT NULL,
    notes TEXT NULL,
    change_type BIGINT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (out_of_service_bed_id) REFERENCES cas1_out_of_service_beds(id),
    FOREIGN KEY (out_of_service_bed_reason_id) REFERENCES cas1_out_of_service_bed_reasons(id),
    FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);
