-- Ideally we'd manage this with the a seed job CSV in 'seed.local+dev+test', but
-- the required configuration for assessment_auto_allocation_username currently
-- differs in local and dev/test

UPDATE cas1_cru_management_areas SET assessment_auto_allocation_username = 'AP_USER_TEST_1';