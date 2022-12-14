-- Bookings
-- Use the original arrival date as the best guess, as the booking must have been created no later than this.
ALTER TABLE bookings ADD COLUMN created_at TIMESTAMP WITH TIME ZONE;

UPDATE bookings
SET created_at = original_arrival_date::timestamp;

ALTER TABLE bookings ALTER COLUMN created_at SET NOT NULL;

-- Extensions
-- Extensions don't have a suitable column to use as a best guess, so use the booking's created_at.
ALTER TABLE extensions ADD COLUMN created_at TIMESTAMP WITH TIME ZONE;

UPDATE extensions
SET created_at = bookings.created_at
FROM bookings
WHERE extensions.booking_id = bookings.id;

ALTER TABLE extensions ALTER COLUMN created_at SET NOT NULL;

-- Arrivals
-- Use the arrival date as the best guess, as the arrival would have been logged after this time.
ALTER TABLE arrivals ADD COLUMN created_at TIMESTAMP WITH TIME ZONE;

UPDATE arrivals
SET created_at = arrival_date::timestamp;

ALTER TABLE arrivals ALTER COLUMN created_at SET NOT NULL;

-- Departures
-- Use the departure date as the best guess, as the departure would have been logged after this time.
ALTER TABLE departures ADD COLUMN created_at TIMESTAMP WITH TIME ZONE;

UPDATE departures
SET created_at = date_time;

ALTER TABLE departures ALTER COLUMN created_at SET NOT NULL;

-- Non-arrivals
-- Use the expected arrival date as the best guess, as the non-arrival would have been logged after this time.
ALTER TABLE non_arrivals ADD COLUMN created_at TIMESTAMP WITH TIME ZONE;

UPDATE non_arrivals
SET created_at = date::timestamp;

ALTER TABLE non_arrivals ALTER COLUMN created_at SET NOT NULL;

-- Cancellations
-- Use the cancellation date as the best guess, as the cancellation would have been logged after this time.
ALTER TABLE cancellations ADD COLUMN created_at TIMESTAMP WITH TIME ZONE;

UPDATE cancellations
SET created_at = date::timestamp;

ALTER TABLE cancellations ALTER COLUMN created_at SET NOT NULL;

-- Confirmations
-- Use the confirmation datetime, as this is currently set to the current time when creating the confirmation row.
ALTER TABLE confirmations ADD COLUMN created_at TIMESTAMP WITH TIME ZONE;

UPDATE confirmations
SET created_at = date_time;

ALTER TABLE confirmations ALTER COLUMN created_at SET NOT NULL;
