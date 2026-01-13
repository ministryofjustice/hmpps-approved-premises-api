CREATE TABLE cas3_overstays (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    previous_departure_date DATE NOT NULL,
    new_departure_date DATE NOT NULL,
    is_authorised BOOLEAN NOT NULL,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_cas3_overstays_booking_id FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE INDEX idx_cas3_overstays_booking_id ON cas3_overstays(booking_id);
