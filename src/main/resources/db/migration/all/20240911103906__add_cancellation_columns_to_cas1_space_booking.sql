ALTER TABLE cas1_space_bookings ADD cancellation_occurred_at date NULL;
ALTER TABLE cas1_space_bookings ADD cancellation_recorded_at timestamptz NULL;
ALTER TABLE cas1_space_bookings ADD cancellation_reason_id UUID NULL;
ALTER TABLE cas1_space_bookings ADD cancellation_reason_notes text NULL;

ALTER TABLE cas1_space_bookings ADD FOREIGN KEY (cancellation_reason_id) REFERENCES cancellation_reasons(id);