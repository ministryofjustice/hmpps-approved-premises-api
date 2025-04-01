-- Ideally we'd manage this with the a seed job CSV in 'seed.local+dev+test', but
-- the required configuration for assessment_auto_allocation_username currently
-- differs in local and dev/test

INSERT INTO cas1_cru_management_area_auto_allocations
SELECT area.id,'MONDAY','JIMSNOWLDAP' FROM cas1_cru_management_areas area WHERE id != 'bfb04c2a-1954-4512-803d-164f7fcf252c'
ON CONFLICT DO NOTHING;

INSERT INTO cas1_cru_management_area_auto_allocations
SELECT area.id,'TUESDAY','JIMSNOWLDAP' FROM cas1_cru_management_areas area WHERE id != 'bfb04c2a-1954-4512-803d-164f7fcf252c'
ON CONFLICT DO NOTHING;

INSERT INTO cas1_cru_management_area_auto_allocations
SELECT area.id,'WEDNESDAY','JIMSNOWLDAP' FROM cas1_cru_management_areas area WHERE id != 'bfb04c2a-1954-4512-803d-164f7fcf252c'
ON CONFLICT DO NOTHING;

INSERT INTO cas1_cru_management_area_auto_allocations
SELECT area.id,'THURSDAY','JIMSNOWLDAP' FROM cas1_cru_management_areas area WHERE id != 'bfb04c2a-1954-4512-803d-164f7fcf252c'
ON CONFLICT DO NOTHING;

INSERT INTO cas1_cru_management_area_auto_allocations
SELECT area.id,'FRIDAY','JIMSNOWLDAP' FROM cas1_cru_management_areas area WHERE id != 'bfb04c2a-1954-4512-803d-164f7fcf252c'
ON CONFLICT DO NOTHING;

INSERT INTO cas1_cru_management_area_auto_allocations
SELECT area.id,'SATURDAY','JIMSNOWLDAP' FROM cas1_cru_management_areas area WHERE id != 'bfb04c2a-1954-4512-803d-164f7fcf252c'
ON CONFLICT DO NOTHING;

INSERT INTO cas1_cru_management_area_auto_allocations
SELECT area.id,'SUNDAY','JIMSNOWLDAP' FROM cas1_cru_management_areas area WHERE id != 'bfb04c2a-1954-4512-803d-164f7fcf252c'
ON CONFLICT DO NOTHING;

-- women's estate
INSERT INTO cas1_cru_management_area_auto_allocations VALUES('bfb04c2a-1954-4512-803d-164f7fcf252c','MONDAY','JIMSNOWLDAP') ON CONFLICT DO NOTHING;
INSERT INTO cas1_cru_management_area_auto_allocations VALUES('bfb04c2a-1954-4512-803d-164f7fcf252c','TUESDAY','JIMSNOWLDAP') ON CONFLICT DO NOTHING;
INSERT INTO cas1_cru_management_area_auto_allocations VALUES('bfb04c2a-1954-4512-803d-164f7fcf252c','WEDNESDAY','JIMSNOWLDAP') ON CONFLICT DO NOTHING;
INSERT INTO cas1_cru_management_area_auto_allocations VALUES('bfb04c2a-1954-4512-803d-164f7fcf252c','THURSDAY','JIMSNOWLDAP') ON CONFLICT DO NOTHING;
INSERT INTO cas1_cru_management_area_auto_allocations VALUES('bfb04c2a-1954-4512-803d-164f7fcf252c','FRIDAY','JIMSNOWLDAP') ON CONFLICT DO NOTHING;
INSERT INTO cas1_cru_management_area_auto_allocations VALUES('bfb04c2a-1954-4512-803d-164f7fcf252c','SATURDAY','JIMSNOWLDAP') ON CONFLICT DO NOTHING;
INSERT INTO cas1_cru_management_area_auto_allocations VALUES('bfb04c2a-1954-4512-803d-164f7fcf252c','SUNDAY','JIMSNOWLDAP') ON CONFLICT DO NOTHING;
