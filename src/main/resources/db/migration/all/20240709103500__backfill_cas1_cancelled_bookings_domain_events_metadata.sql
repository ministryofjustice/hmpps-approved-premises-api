INSERT INTO domain_events_metadata
    (domain_event_id, name, value)
SELECT
    domain_events.id , 'CAS1_CANCELLATION_ID', cancellations.id
FROM
    cancellations
INNER JOIN
    bookings ON cancellations.booking_id = bookings.id
INNER JOIN
    domain_events ON bookings.id = domain_events.booking_id
WHERE
    domain_events.type = 'APPROVED_PREMISES_BOOKING_CANCELLED'
AND
    cancellations.id::text
	NOT IN
		(
		SELECT DISTINCT value
		FROM
		    domain_events_metadata
        WHERE
            name = 'CAS1_CANCELLATION_ID'
        )