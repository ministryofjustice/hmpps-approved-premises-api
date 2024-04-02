-- The probation area codes returned for test users by the locally deployed community API do not exist
-- in our map probation_area_probation_region_mappings table. Specifically, 'GCS' is returned for our
-- test user 'jim snow'.
--
-- Because of this, every time we check if the user needs updating the resolved probation_region doesn't
-- match (it always maps to 'null'), and because of this the user is updated.
--
-- Ideally we'd fix our local community_api deployment to use expected region codes for test users.
-- For reference, in community_api the current area codes are defined by community_api in V1_2__stafforganisations_data.sql
-- and test users defined in V1_4__offenders_licences_data.sql
INSERT INTO probation_area_probation_region_mappings(id, probation_area_delius_code, probation_region_id) VALUES
   ('49584578-b743-4f55-a2fe-55a1790d74ba', 'GCS', '43606be0-9836-441d-9bc1-5586de9ac931');