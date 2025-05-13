ALTER TABLE cas1_space_bookings
    DROP COLUMN transferred_to;

ALTER TABLE cas1_space_bookings
    ADD COLUMN transferred_from UUID;

ALTER TABLE cas1_space_bookings
    ADD CONSTRAINT fk_transferred_to
        FOREIGN KEY (transferred_from) REFERENCES cas1_space_bookings(id);

CREATE INDEX cas1_space_bookings_transferred_from_idx ON cas1_space_bookings (transferred_from);
