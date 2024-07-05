-- This will only backfill the most recent booking made for a placement
-- request, but for reporting purposes this is adequate
INSERT INTO domain_events_metadata (domain_event_id, name, value)
SELECT
    evt.id as id,
    'CAS1_PLACEMENT_REQUEST_ID',
    cast(pr.id as TEXT)
FROM domain_events evt
         INNER JOIN placement_requests pr on pr.booking_id = evt.booking_id
WHERE evt.type = 'APPROVED_PREMISES_BOOKING_MADE';