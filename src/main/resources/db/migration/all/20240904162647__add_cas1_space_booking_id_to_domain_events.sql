ALTER TABLE domain_events ADD cas1_space_booking_id UUID NULL;
ALTER TABLE domain_events ADD FOREIGN KEY (cas1_space_booking_id) REFERENCES cas1_space_bookings(id);
CREATE INDEX domain_events_cas1_space_booking_id_idx ON domain_events (cas1_space_booking_id);