INSERT INTO domain_events_metadata(domain_event_id,name,value)
SELECT
    d.id,
    'CAS1_REQUESTED_AP_TYPE',
    apa.ap_type
FROM
    domain_events d
        INNER JOIN approved_premises_applications apa ON apa.id = d.application_id
WHERE
    d.type = 'APPROVED_PREMISES_APPLICATION_SUBMITTED' AND
    d.id not in (
        select distinct domain_event_id from domain_events_metadata where name = 'CAS1_REQUESTED_AP_TYPE'
    );