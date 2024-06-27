-- we delete any existing entries because previously implementations of the UI populating
-- this field was using the incorrect value
DELETE FROM domain_events_metadata WHERE name = 'CAS1_REQUESTED_AP_TYPE' AND domain_event_id in (
    select distinct id from domain_events where type = 'APPROVED_PREMISES_APPLICATION_ASSESSED'
);

INSERT INTO domain_events_metadata(domain_event_id,name,value)
SELECT
    d.id,
    'CAS1_REQUESTED_AP_TYPE',
    CASE
        WHEN assessment.data -> 'matching-information' -> 'matching-information' ->> 'apType' = 'normal' THEN 'NORMAL'
        WHEN assessment.data -> 'matching-information' -> 'matching-information' ->> 'apType' = 'isPIPE' THEN 'PIPE'
        WHEN assessment.data -> 'matching-information' -> 'matching-information' ->> 'apType' = 'isESAP' THEN 'ESAP'
        WHEN assessment.data -> 'matching-information' -> 'matching-information' ->> 'apType' = 'isRecoveryFocussed' THEN 'RFAP'
        WHEN assessment.data -> 'matching-information' -> 'matching-information' ->> 'apType' = 'isMHAPElliottHouse' THEN 'MHAP_ST_JOSEPHS'
        WHEN assessment.data -> 'matching-information' -> 'matching-information' ->> 'apType' = 'isMHAPStJosephs' THEN 'MHAP_ELLIOTT_HOUSE'
        ELSE assessment.data -> 'matching-information' -> 'matching-information' ->> 'apType'
        END
FROM domain_events d
         INNER JOIN assessments assessment ON assessment.id = d.assessment_id
WHERE d.type = 'APPROVED_PREMISES_APPLICATION_ASSESSED' AND
    d.id not in (
        select distinct domain_event_id from domain_events_metadata where name = 'CAS1_REQUESTED_AP_TYPE'
    );