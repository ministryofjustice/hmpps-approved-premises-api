INSERT INTO domain_events_metadata(domain_event_id,name,value)
SELECT
    domain_events.id,
    'CAS1_PLACEMENT_APPLICATION_ID',
    domain_events.data -> 'eventDetails' ->> 'placementApplicationId'
FROM
    domain_events
WHERE
    domain_events.type = 'APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN' AND
    domain_events.id not in (
        select distinct domain_event_id from domain_events_metadata where name = 'CAS1_PLACEMENT_APPLICATION_ID'
    );