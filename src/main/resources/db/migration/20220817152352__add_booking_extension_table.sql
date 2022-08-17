CREATE TABLE extensions (
    id UUID NOT NULL,
    booking_id UUID NOT NULL,
    previous_departure_date DATE NOT NULL,
    new_departure_date DATE NOT NULL,
    notes TEXT,
    PRIMARY KEY (id),
    FOREIGN KEY (booking_id) REFERENCES bookings(id)
);
