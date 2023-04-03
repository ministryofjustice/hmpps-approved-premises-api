CREATE TABLE booking_not_mades (
    id UUID NOT NULL,
    placement_request_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    notes TEXT,
    PRIMARY KEY (id),
    FOREIGN KEY (placement_request_id) REFERENCES placement_requests(id)
);
