CREATE TABLE bed_moves (
    id uuid NOT NULL,
    booking_id uuid NOT NULL,
    previous_bed_id uuid NOT NULL,
    new_bed_id uuid NOT NULL,
    notes TEXT NULL,
    created_at timestamp NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (booking_id) REFERENCES bookings(id),
    FOREIGN KEY (previous_bed_id) REFERENCES beds(id),
    FOREIGN KEY (new_bed_id) REFERENCES beds(id)
);