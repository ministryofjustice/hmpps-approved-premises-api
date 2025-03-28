ALTER TABLE cas1_space_bookings
    ADD COLUMN transferred_to UUID;

ALTER TABLE cas1_space_bookings
    ADD CONSTRAINT fk_transferred_to
FOREIGN KEY (transferred_to) REFERENCES cas1_space_bookings(id);