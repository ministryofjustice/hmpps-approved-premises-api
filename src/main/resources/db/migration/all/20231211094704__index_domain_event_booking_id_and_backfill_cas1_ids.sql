CREATE INDEX ON domain_events(booking_id);

UPDATE domain_events SET booking_id = cast(data -> 'eventDetails' ->> 'bookingId' as UUID) WHERE type = 'APPROVED_PREMISES_BOOKING_MADE';