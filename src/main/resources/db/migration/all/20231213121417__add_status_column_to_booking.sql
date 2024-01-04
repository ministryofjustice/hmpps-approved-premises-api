ALTER TABLE bookings ADD COLUMN status TEXT;
CREATE INDEX idx_bookings_status ON bookings (status);