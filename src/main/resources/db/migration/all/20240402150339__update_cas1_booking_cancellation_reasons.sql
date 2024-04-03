UPDATE cancellation_reasons
SET is_active = false
WHERE service_scope = 'approved-premises'
  AND name IN ('The placement is being transferred','The AP requested it');

UPDATE cancellation_reasons
SET name = 'Person has been deprioritised at this AP'
WHERE service_scope = 'approved-premises' AND name = 'The placement has been deprioritised (it''s a lower tier)';

UPDATE cancellation_reasons
SET name = 'Probation Practitioner requested it'
WHERE service_scope = 'approved-premises' AND name = 'The probation practitioner requested it';

UPDATE cancellation_reasons
SET name = 'AP has appealed placement'
WHERE service_scope = 'approved-premises' AND name = 'The assessment is being appealed';

INSERT INTO cancellation_reasons (id, name, is_active, service_scope)
VALUES
    ('c4ae53be-8bf6-4139-b530-254eb79bf79f', 'An alternative AP placement needs to be found', true, 'approved-premises');

UPDATE cancellation_reasons SET sort_order = 99 WHERE service_scope = 'approved-premises' and name = 'Other';
