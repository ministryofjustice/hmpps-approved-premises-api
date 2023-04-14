ALTER TABLE departure_reasons DROP CONSTRAINT departure_reason_name_key;
ALTER TABLE departure_reasons ADD CONSTRAINT departure_reason_active_name_service_scope_unique UNIQUE (name, is_active, service_scope);

UPDATE departure_reasons
SET is_active = FALSE
WHERE service_scope = '*'
AND name <> 'Other';

UPDATE departure_reasons
SET name = 'Bed Withdrawn - Further custodial sentence imposed'
WHERE name = 'Further custodial sentence imposed'
AND service_scope = 'temporary-accommodation';

INSERT INTO departure_reasons(id, name, is_active, service_scope, legacy_delius_reason_code)
VALUES
    -- Temporary Accommodation (different naming to former shared reasons or new)
    ('b5d8a978-eb20-436e-a00f-f1358a3664d5', 'Admitted to Hospital/Healthcare Facility', TRUE, 'temporary-accommodation', NULL),
    ('1de629ea-c5fb-4e91-9b0b-ef7d4d69bd5b', 'Bed Withdrawn - Serious Incident related to CAS3 placement', TRUE, 'temporary-accommodation', NULL),
    ('d6c85a7e-cf9d-4a47-8da2-a25356d34570', 'Bed Withdrawn – Person on probation no longer in property', TRUE, 'temporary-accommodation', NULL),
    ('93153ad1-38a3-4f4f-809e-ba8297c0565a', 'Bed Withdrawn – Further offence/behaviour/increasing risk', TRUE, 'temporary-accommodation', NULL),
    ('94976884-c32f-4162-b3af-214beed0e988', 'Bed Withdrawn - Recall/Breach', TRUE, 'temporary-accommodation', NULL),
    ('b8975a4d-68ff-4f42-9ac8-5136c4b393ef', 'Deceased', TRUE, 'temporary-accommodation', NULL),
    ('166b6b02-6f42-4447-b278-dd9a0b437b54', 'Planned Move on', TRUE, 'temporary-accommodation', NULL),
    ('529fe9a0-a644-48db-9e62-83c83a623a93', 'Person on probation moved to another CAS3 property', TRUE, 'temporary-accommodation', NULL),
    -- Approved Premises (no longer used by Temporary Accommodation)
    ('812e5fd3-de05-40c9-b261-0a9a12c2deee', 'Breach / recall (curfew)', TRUE, 'approved-premises', 'F'),
    ('6761f2a5-4a83-4c2b-a9c8-d6906a7f6e8d', 'Breach / recall (house rules)', TRUE, 'approved-premises', 'B'),
    ('1d0cbc0a-9625-438a-a598-c8c8ac284c0d', 'Breach / recall (licence or bail condition)', TRUE, 'approved-premises', 'E'),
    ('0de457c6-8938-47b6-9de8-723fbc4dd408', 'Breach / recall (positive drugs test)', TRUE, 'approved-premises', 'C'),
    ('2700152b-b904-449e-a6fd-50ae24d6fc7e', 'Left of own volition', TRUE, 'approved-premises', 'K'),
    -- Approved Premises (former shared reasons)
    ('0c9102eb-5321-4b94-a4da-4884a29cd637', 'Admitted to Hospital', TRUE, 'approved-premises', 'Q'),
    ('0f856559-26c0-4184-96fe-00d446e91da2', 'Bed Withdrawn', TRUE, 'approved-premises', 'X'),
    ('f20dd19b-d134-4abd-87a7-b4e5722c0729', 'Breach / recall (abscond)', TRUE, 'approved-premises', 'A'),
    ('7fa5f895-a504-4cdb-93dd-b6c4029484e1', 'Breach / recall (behaviour / increasing risk)', TRUE, 'approved-premises', 'D'),
    ('012f21fa-1c46-4dca-ab8d-2e73cbe5ec84', 'Breach / recall (other)', TRUE, 'approved-premises', 'G'),
    ('a9f64800-9f16-4096-b8f1-b03960fc728a', 'Died', TRUE, 'approved-premises', 'P'),
    ('1bfe5cdf-348e-4a6e-8414-177a92a53d26', 'Planned move-on', TRUE, 'approved-premises', 'O');
