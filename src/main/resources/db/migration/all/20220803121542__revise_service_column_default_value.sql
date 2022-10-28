UPDATE premises
SET service = 'approved-premises'
WHERE service = 'CAS1';

UPDATE premises
SET service='temporary-accommodation'
WHERE service = 'CAS3';