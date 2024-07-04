DELETE FROM domain_events_metadata
WHERE
    domain_event_id IN (
        SELECT ID FROM domain_events
        WHERE domain_events.type = 'APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_ASSESSED'
     );

DELETE FROM domain_events
WHERE
    domain_events.type = 'APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_ASSESSED';