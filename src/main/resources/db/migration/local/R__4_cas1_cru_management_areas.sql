-- Ideally we'd manage this with the a seed job CSV in 'seed.local+dev+test', but
-- the required configuration for assessment_auto_allocation_username currently
-- differs in local and dev/test

UPDATE cas1_cru_management_areas SET assessment_auto_allocation_username = 'JIMSNOWLDAP';
UPDATE cas1_cru_management_areas SET assessment_auto_allocation_username = 'CRUWOMENSESTATE' WHERE id = 'bfb04c2a-1954-4512-803d-164f7fcf252c';