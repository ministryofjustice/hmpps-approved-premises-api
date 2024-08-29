ALTER TABLE cas1_space_bookings RENAME COLUMN arrival_date TO expected_arrival_date;
ALTER TABLE cas1_space_bookings RENAME COLUMN departure_date TO expected_departure_date;

ALTER TABLE cas1_space_bookings ADD actual_arrival_date_time timestamptz NULL;
ALTER TABLE cas1_space_bookings ADD actual_departure_date_time timestamptz NULL;
ALTER TABLE cas1_space_bookings ADD canonical_arrival_date date NOT NULL;
ALTER TABLE cas1_space_bookings ADD canonical_departure_date date NOT NULL;
ALTER TABLE cas1_space_bookings ADD crn text NOT NULL;
ALTER TABLE cas1_space_bookings ADD key_worker_staff_code text NULL;
ALTER TABLE cas1_space_bookings ADD key_worker_assigned_at timestamptz NULL;
