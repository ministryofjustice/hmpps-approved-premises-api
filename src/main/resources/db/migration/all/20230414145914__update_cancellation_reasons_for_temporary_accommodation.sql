UPDATE cancellation_reasons
SET name = 'Alternative suitable accommodation provided by Local Authority'
WHERE name = 'Alternative suitable accommodation provided by LHA'
AND service_scope = 'temporary-accommodation';

UPDATE cancellation_reasons
SET name = 'Alternative suitable accommodation provided - Other (inc. AP, CAS2, friends/family etc.)'
WHERE name = 'Alternative suitable accommodation provided (Other)'
AND service_scope = 'temporary-accommodation';

UPDATE cancellation_reasons
SET name = 'Person on probation rejected placement'
WHERE name = 'Person on probation rejected placement (Out of area)'
AND service_scope = 'temporary-accommodation';

UPDATE cancellation_reasons
SET name = 'Withdrawn by referrer (e.g. recalled, further custody, placement related risk concern)'
WHERE name = 'Withdrawn by referrer'
AND service_scope = 'temporary-accommodation';

INSERT INTO cancellation_reasons(id, name, is_active, service_scope)
VALUES ('2a7fc443-2f31-4501-90c4-435a6e8e59d3', 'Supplier unable to accommodate (Please explain in notes)', TRUE, 'temporary-accommodation');
