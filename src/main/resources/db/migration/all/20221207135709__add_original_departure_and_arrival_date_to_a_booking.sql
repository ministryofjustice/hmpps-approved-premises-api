ALTER TABLE bookings ADD COLUMN original_arrival_date DATE;
ALTER TABLE bookings ADD COLUMN original_departure_date DATE;

UPDATE bookings
SET
    original_arrival_date = arrival_date,
    original_departure_date = departure_date;

ALTER TABLE bookings ALTER COLUMN original_arrival_date SET NOT NULL;
ALTER TABLE bookings ALTER COLUMN original_departure_date SET NOT NULL;
