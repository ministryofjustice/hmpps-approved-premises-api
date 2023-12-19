UPDATE domain_events SET booking_id = cast(data -> 'eventDetails' ->> 'bookingId' as UUID) WHERE type = 'APPROVED_PREMISES_PERSON_ARRIVED';
UPDATE domain_events SET booking_id = cast(data -> 'eventDetails' ->> 'bookingId' as UUID) WHERE type = 'APPROVED_PREMISES_BOOKING_CANCELLED';
UPDATE domain_events SET booking_id = cast(data -> 'eventDetails' ->> 'bookingId' as UUID) WHERE type = 'APPROVED_PREMISES_PERSON_DEPARTED';
UPDATE domain_events SET booking_id = cast(data -> 'eventDetails' ->> 'bookingId' as UUID) WHERE type = 'APPROVED_PREMISES_PERSON_NOT_ARRIVED';