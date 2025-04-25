CREATE INDEX cas1_space_bookings_transferred_to_idx ON cas1_space_bookings (transferred_to);
ALTER TABLE cas1_space_bookings ADD transfer_type text NULL;