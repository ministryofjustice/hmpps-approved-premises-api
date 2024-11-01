ALTER TABLE cas1_space_bookings ADD non_arrival_confirmed_at timestamptz NULL;
ALTER TABLE cas1_space_bookings ADD non_arrival_notes text NULL;
ALTER TABLE cas1_space_bookings ADD non_arrival_reason_id UUID NULL;

ALTER TABLE cas1_space_bookings ADD FOREIGN KEY (non_arrival_reason_id) REFERENCES non_arrival_reasons(id);