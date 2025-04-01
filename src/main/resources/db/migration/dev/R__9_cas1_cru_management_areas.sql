-- Ideally we'd manage this with the a seed job CSV in 'seed.local+dev+test', but
-- the required configuration for assessment_auto_allocation_username currently
-- differs in local and dev/test

UPDATE cas1_cru_management_areas SET assessment_auto_allocation_username = 'AP_USER_TEST_1';

INSERT INTO cas1_cru_management_area_auto_allocations
SELECT area.id,'MONDAY','AP_USER_TEST_1' FROM cas1_cru_management_areas area
ON CONFLICT DO NOTHING;

INSERT INTO cas1_cru_management_area_auto_allocations
SELECT area.id,'TUESDAY','AP_USER_TEST_1' FROM cas1_cru_management_areas area
ON CONFLICT DO NOTHING;

INSERT INTO cas1_cru_management_area_auto_allocations
SELECT area.id,'WEDNESDAY','AP_USER_TEST_1' FROM cas1_cru_management_areas area
ON CONFLICT DO NOTHING;

INSERT INTO cas1_cru_management_area_auto_allocations
SELECT area.id,'THURSDAY','AP_USER_TEST_1' FROM cas1_cru_management_areas area
ON CONFLICT DO NOTHING;

INSERT INTO cas1_cru_management_area_auto_allocations
SELECT area.id,'FRIDAY','AP_USER_TEST_1' FROM cas1_cru_management_areas area
ON CONFLICT DO NOTHING;

INSERT INTO cas1_cru_management_area_auto_allocations
SELECT area.id,'SATURDAY','AP_USER_TEST_1' FROM cas1_cru_management_areas area
ON CONFLICT DO NOTHING;

INSERT INTO cas1_cru_management_area_auto_allocations
SELECT area.id,'SUNDAY','AP_USER_TEST_1' FROM cas1_cru_management_areas area
ON CONFLICT DO NOTHING;