ALTER TABLE bookings DROP COLUMN key_worker_id;
DROP TABLE key_workers;
ALTER TABLE bookings ADD COLUMN key_worker_staff_id BIGINT;
