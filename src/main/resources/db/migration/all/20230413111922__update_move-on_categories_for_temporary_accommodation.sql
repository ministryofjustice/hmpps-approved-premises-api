UPDATE move_on_categories
SET name = 'Transient/Short term accommodation'
WHERE name = 'Transient/short term accommodation'
AND service_scope = 'temporary-accommodation';

UPDATE move_on_categories
SET name = 'Accommodation secured via AfEO – Accommodation for Ex-Offenders Scheme'
WHERE name = 'AfEO Funded'
AND service_scope = 'temporary-accommodation';

INSERT INTO move_on_categories(id, name, is_active, service_scope, legacy_delius_category_code)
VALUES
    ('bd7425b0-ee9a-491c-a64b-c6a034847778', 'CAS2', TRUE, 'temporary-accommodation', NULL),
    ('b6d65622-44ae-42ac-9da0-c6a02532c3d5', 'Hospital', TRUE, 'temporary-accommodation', NULL),
    ('c532986f-462c-4adf-ab2e-583e49f06ec6', 'Prison (further custodial event)', TRUE, 'temporary-accommodation', NULL),
    ('8c392f36-515c-4210-bb72-6255f12abb91', 'Recalled', TRUE, 'temporary-accommodation', NULL),
    ('5d1f4d82-6830-43bf-b197-c5975c0c721b', 'N/A (only to be used where person on probation is deceased)', TRUE, 'temporary-accommodation', NULL),
    ('24e80792-1eee-48fc-9a02-51275c3a6217', 'N/A (only to be used where person on probation has moved to another CAS3 property)', TRUE, 'temporary-accommodation', NULL);
