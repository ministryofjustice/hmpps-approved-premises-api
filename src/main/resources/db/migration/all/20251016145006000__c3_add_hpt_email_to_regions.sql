ALTER TABLE probation_regions
    ADD COLUMN IF NOT EXISTS hpt_email text;

UPDATE probation_regions
SET hpt_email = 'londonps.hpt@justice.gov.uk'
WHERE delius_code = 'N07';

UPDATE probation_regions
SET hpt_email = 'emprobationtaskforce@justice.gov.uk'
WHERE delius_code = 'N53';

UPDATE probation_regions
SET hpt_email = 'wmprobationtaskforce@justice.gov.uk'
WHERE delius_code = 'N52';

UPDATE probation_regions
SET hpt_email = 'nwhpt@justice.gov.uk'
WHERE delius_code = 'N51';

UPDATE probation_regions
SET hpt_email = 'nehpt@justice.gov.uk'
WHERE delius_code = 'N54';

UPDATE probation_regions
SET hpt_email = 'kssps.hpt@justice.gov.uk'
WHERE delius_code = 'N57';

UPDATE probation_regions
SET hpt_email = 'gmhpt@justice.gov.uk'
WHERE delius_code = 'N50';

UPDATE probation_regions
SET hpt_email = 'eastofenglandps.hpt@justice.gov.uk'
WHERE delius_code = 'N56';

UPDATE probation_regions
SET hpt_email = 'swps.hpt@justice.gov.uk'
WHERE delius_code = 'N58';

UPDATE probation_regions
SET hpt_email = 'scprobationtaskforce@justice.gov.uk'
WHERE delius_code = 'N59';

UPDATE probation_regions
SET hpt_email = 'wales.probationtaskforce@justice.gov.uk'
WHERE delius_code = 'N03';

UPDATE probation_regions
SET hpt_email = 'yathps.hpt@justice.gov.uk'
WHERE delius_code = 'N55';