CREATE TABLE date_changes (
    id UUID NOT NULL,
    booking_id UUID NOT NULL,
    changed_by_user_id UUID NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    previous_arrival_date DATE NOT NULL,
    previous_departure_date DATE NOT NULL,
    new_arrival_date DATE NOT NULL,
    new_departure_date DATE NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (booking_id) REFERENCES bookings(id),
    FOREIGN KEY (changed_by_user_id) REFERENCES users(id)
);
