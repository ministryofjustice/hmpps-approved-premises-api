CREATE TABLE cas1_space_bookings (
    id UUID NOT NULL,
    premises_id UUID NOT NULL,
    placement_request_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    arrival_date DATE NOT NULL,
    departure_date DATE NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (premises_id) REFERENCES approved_premises(premises_id),
    FOREIGN KEY (placement_request_id) REFERENCES placement_requests(id),
    FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE INDEX ON cas1_space_bookings(premises_id, placement_request_id);
