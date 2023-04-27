UPDATE characteristics
SET name = 'Ground floor level access'
WHERE name = 'Floor level access'
AND service_scope = 'temporary-accommodation';

UPDATE characteristics
SET is_active = FALSE
WHERE name = 'Not suitable for arson offenders'
AND service_scope = 'temporary-accommodation';

UPDATE characteristics
SET is_active = FALSE
WHERE name = 'Lift access'
AND service_scope = 'temporary-accommodation';

UPDATE characteristics
SET is_active = FALSE
WHERE name = 'Single bed'
AND service_scope = 'temporary-accommodation';

UPDATE characteristics
SET is_active = FALSE
WHERE name = 'Double bed'
AND service_scope = 'temporary-accommodation';

UPDATE characteristics
SET is_active = FALSE
WHERE name = 'Not suitable for registered sex offenders (RSO)'
AND service_scope = 'temporary-accommodation';

INSERT INTO characteristics(id, name, service_scope, model_scope, property_name, is_active)
VALUES
    ('c0fc6e07-4ca3-45ac-88ea-375479e1419f', 'Not suitable for those with an arson history', 'temporary-accommodation', 'premises', NULL, TRUE),
    ('cb7447cd-d629-4c89-be93-cd48c1060af8', 'Not suitable for those who pose a sexual risk to adults', 'temporary-accommodation', 'premises', NULL, TRUE),
    ('8fcf6f60-bbca-4425-b0e8-9dfbe88f3aa6', 'Not suitable for those who pose a sexual risk to children', 'temporary-accommodation', 'premises', NULL, TRUE),
    ('454a5ff4-d87a-43f9-8989-135bcc47a635', 'Single occupancy', 'temporary-accommodation', 'premises', NULL, TRUE),
    ('62a38d3a-4797-4b0f-8681-7befea1035a4', 'Shared property', 'temporary-accommodation', 'premises', NULL, TRUE),
    ('e2868d4f-cb8f-4a4b-ae27-6b887a63d37c', 'Shared entrance', 'temporary-accommodation', 'premises', NULL, TRUE),
    ('da0e86b3-fce8-42b6-af0c-ac6f905611e6', 'Lift available', 'temporary-accommodation', 'premises', NULL, TRUE),
    ('1fb6000b-7c55-44cc-bb8c-38835c99ab99', 'Sensitive let', 'temporary-accommodation', 'premises', NULL, TRUE),
    ('7f805b33-048e-4034-9ded-21947bf5c728', 'Close proximity', 'temporary-accommodation', 'premises', NULL, TRUE),
    ('78adf0c6-01cb-4b1a-be3e-1a3f5346a497', 'Rural/out of town', 'temporary-accommodation', 'premises', NULL, TRUE),
    ('9fb21592-7cca-4ef2-98ca-5fcef22c8ee6', 'Other – please state in notes', 'temporary-accommodation', '*', NULL, TRUE);
