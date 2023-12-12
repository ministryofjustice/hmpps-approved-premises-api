CREATE INDEX idx_cancellations_booking_id ON cancellations (booking_id);
CREATE INDEX idx_departures_booking_id ON departures (booking_id);
CREATE INDEX idx_arrivals_booking_id ON arrivals (booking_id);
CREATE INDEX idx_confirmations_booking_id ON confirmations (booking_id);
CREATE INDEX idx_non_arrivals_booking_id ON non_arrivals (booking_id);
