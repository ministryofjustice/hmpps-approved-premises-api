CREATE TABLE cas1_space_bookings_criteria (
  space_booking_id UUID NOT NULL,
  characteristic_id UUID NOT NULL,
  PRIMARY KEY (space_booking_id, characteristic_id),
  FOREIGN KEY (space_booking_id) REFERENCES cas1_space_bookings (id),
  FOREIGN KEY (characteristic_id) REFERENCES characteristics (id)
);