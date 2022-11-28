CREATE TABLE confirmations (
    id UUID NOT NULL,
    booking_id UUID NOT NULL,
    date_time TIMESTAMP WITH TIME ZONE NOT NULL,
    notes TEXT,
    PRIMARY KEY (id),
    FOREIGN KEY (booking_id) REFERENCES bookings(id)
);
