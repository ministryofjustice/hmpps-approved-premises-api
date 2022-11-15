ALTER TABLE bookings ADD COLUMN bed_id UUID;
ALTER TABLE bookings ADD COLUMN service TEXT;
ALTER TABLE bookings ADD FOREIGN KEY (bed_id) REFERENCES beds (id);

UPDATE bookings
SET service = 'approved-premises'
WHERE premises_id IN (
    SELECT id
    FROM premises
    WHERE service = 'approved-premises'
);

UPDATE bookings
SET service = 'temporary-accommodation'
WHERE premises_id IN (
    SELECT id
    FROM premises
    WHERE service = 'temporary-accommodation'
);

ALTER TABLE bookings ALTER COLUMN service SET NOT NULL;
