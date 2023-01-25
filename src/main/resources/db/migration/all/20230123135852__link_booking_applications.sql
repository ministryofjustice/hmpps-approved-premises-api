ALTER TABLE bookings ADD COLUMN application_id UUID;
ALTER TABLE bookings ADD CONSTRAINT application_id_fk FOREIGN KEY (application_id) REFERENCES applications(id);
ALTER TABLE bookings ADD COLUMN offline_application_id UUID;
ALTER TABLE bookings ADD CONSTRAINT offline_application_id_fk FOREIGN KEY (offline_application_id) REFERENCES offline_applications(id);
