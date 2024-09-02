ALTER TABLE cas1_space_bookings ADD key_worker_name TEXT NULL;
ALTER TABLE cas1_space_bookings ADD approved_premises_application_id UUID NULL;

ALTER TABLE cas1_space_bookings ADD FOREIGN KEY (approved_premises_application_id) REFERENCES approved_premises_applications(id);