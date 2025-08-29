
UPDATE move_on_categories
SET is_active = false
WHERE name = 'Accommodation secured via AfEO – Accommodation for Ex-Offenders Scheme' AND service_scope = 'temporary-accommodation';

UPDATE move_on_categories
SET name = 'CAS1/AP'
WHERE name = 'Approved Premises' AND service_scope = 'temporary-accommodation';

UPDATE move_on_categories
SET name = 'Friends or family (settled)'
WHERE name = 'Friends/Family (settled)' AND service_scope = 'temporary-accommodation';

UPDATE move_on_categories
SET name = 'Friends or family (transient)'
WHERE name = 'Friends/Family (transient)' AND service_scope = 'temporary-accommodation';

UPDATE move_on_categories
SET name = 'Homeless – rough sleeping'
WHERE name = 'Homeless - Rough Sleeping' AND service_scope = 'temporary-accommodation';

UPDATE move_on_categories
SET name = 'Homeless – shelter/emergency hostel/campsite'
WHERE name = 'Homeless - Shelter/Emergency Hostel/Campsite' AND service_scope = 'temporary-accommodation';

UPDATE move_on_categories
SET name = 'Homeless – squat'
WHERE name = 'Homeless - Squat' AND service_scope = 'temporary-accommodation';

UPDATE move_on_categories
SET name = 'Householder (owner – freehold or leasehold)'
WHERE name = 'Householder (Owner - freehold or leasehold)' AND service_scope = 'temporary-accommodation';

UPDATE move_on_categories
SET name = 'Long term residential healthcare'
WHERE name = 'Long Term Residential Healthcare' AND service_scope = 'temporary-accommodation';

UPDATE move_on_categories
SET name = 'Pending (awaiting move on outcome in NDelius – to be updated)'
WHERE name = 'Pending (no category has been added to NDelius)' AND service_scope = 'temporary-accommodation';

UPDATE move_on_categories
SET is_active = false
WHERE name = 'They’re unlawfully at large or at large' AND service_scope = 'temporary-accommodation';

UPDATE move_on_categories
SET name = 'Support housing'
WHERE name = 'Supported Housing' AND service_scope = 'temporary-accommodation';

INSERT INTO move_on_categories(id, name, is_active, service_scope) VALUES
  ('b3a5bf51-f5a3-46fb-8de5-c0f1b1ee1e8c', 'Updated move on outcome - not recorded in NDelius', true, 'temporary-accommodation');
