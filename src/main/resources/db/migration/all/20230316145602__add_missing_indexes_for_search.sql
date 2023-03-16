CREATE INDEX ON rooms(premises_id);
CREATE INDEX ON premises(point);
CREATE INDEX ON premises_characteristics(premises_id);
CREATE INDEX ON beds(room_id);
CREATE INDEX ON lost_beds(bed_id);
CREATE INDEX ON lost_bed_cancellations(lost_bed_id);
CREATE INDEX ON bookings(bed_id);
CREATE INDEX ON cancellations(booking_id);
