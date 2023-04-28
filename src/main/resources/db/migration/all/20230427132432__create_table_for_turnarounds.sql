CREATE TABLE turnarounds (
    id UUID NOT NULL,
    booking_id UUID NOT NULL,
    working_day_count INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (booking_id) REFERENCES bookings(id)
);
