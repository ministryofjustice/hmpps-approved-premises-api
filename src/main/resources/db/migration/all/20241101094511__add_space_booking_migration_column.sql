ALTER TABLE cas1_space_bookings ADD migrated_from_booking_id UUID NULL;
ALTER TABLE cas1_space_bookings ADD FOREIGN KEY (migrated_from_booking_id) REFERENCES bookings(id);