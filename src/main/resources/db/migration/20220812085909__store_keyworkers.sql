CREATE TABLE key_workers (
    id UUID NOT NULL,
    name TEXT NOT NULL,
    is_active BOOL NOT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE bookings DROP COLUMN key_worker;
ALTER TABLE bookings ADD COLUMN key_worker_id UUID NOT NULL;
ALTER TABLE bookings ADD CONSTRAINT key_worker_fk FOREIGN KEY (key_worker_id) REFERENCES key_workers(id);
