UPDATE cancellation_reasons
SET is_active = false
WHERE service_scope = 'approved-premises'
AND name IN ('Deceased','Error in Booking Details');

UPDATE cancellation_reasons
SET name = 'The placement has been deprioritised (it''s a lower tier)'
WHERE service_scope = 'approved-premises' AND name = 'Placement Prioritisation';

UPDATE cancellation_reasons
SET name = 'The probation practitioner requested it'
WHERE service_scope = 'approved-premises' AND name = 'Withdrawn by Probation Practitioner';

INSERT INTO cancellation_reasons (id, name, is_active, service_scope)
VALUES
    ('b43a314a-7e25-459b-8a82-7bbcc9313caa', 'The assessment is being appealed', true, 'approved-premises'),
    ('b5688d29-762d-499c-be42-708729aef5ed', 'The placement is being transferred', true, 'approved-premises'),
    ('3c2a6820-d59d-4c06-a194-7873e9a7b63a', 'The AP requested it', true, 'approved-premises');
