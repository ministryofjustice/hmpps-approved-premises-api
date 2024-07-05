INSERT INTO domain_events_metadata (domain_event_id, name, value)
SELECT
    evt.id as id,
    'CAS1_PLACEMENT_REQUEST_ID',
    cast(bnm.placement_request_id as TEXT)
FROM domain_events evt
INNER JOIN booking_not_mades bnm on bnm.created_at = evt.occurred_at
WHERE evt.type = 'APPROVED_PREMISES_BOOKING_NOT_MADE';