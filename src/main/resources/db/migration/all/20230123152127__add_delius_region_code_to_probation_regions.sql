ALTER TABLE probation_regions ADD COLUMN delius_code TEXT;
ALTER TABLE probation_regions ADD CONSTRAINT delius_code_unique UNIQUE (delius_code);

UPDATE probation_regions
SET delius_code = 'N53'
WHERE "name" = 'East Midlands';

UPDATE probation_regions
SET delius_code = 'N56'
WHERE "name" = 'East of England';

UPDATE probation_regions
SET delius_code = 'MCG'
WHERE "name" = 'Greater Manchester';

UPDATE probation_regions
SET delius_code = 'N57'
WHERE "name" = 'Kent, Surrey & Sussex';

UPDATE probation_regions
SET delius_code = 'N07'
WHERE "name" = 'London';

UPDATE probation_regions
SET delius_code = 'N54'
WHERE "name" = 'North East';

UPDATE probation_regions
SET delius_code = 'N51'
WHERE "name" = 'North West';

UPDATE probation_regions
SET delius_code = 'N59'
WHERE "name" = 'South Central';

UPDATE probation_regions
SET delius_code = 'N58'
WHERE "name" = 'South West';

UPDATE probation_regions
SET delius_code = 'N03'
WHERE "name" = 'Wales';

UPDATE probation_regions
SET delius_code = 'N52'
WHERE "name" = 'West Midlands';

UPDATE probation_regions
SET delius_code = 'N55'
WHERE "name" = 'Yorkshire & The Humber';

ALTER TABLE probation_regions ALTER COLUMN delius_code SET NOT NULL;
