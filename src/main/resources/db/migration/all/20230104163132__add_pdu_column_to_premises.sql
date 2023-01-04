ALTER TABLE temporary_accommodation_premises ADD COLUMN pdu TEXT;

-- Some TA premises may have existing rows on the table.
UPDATE temporary_accommodation_premises
SET pdu = 'Not specified';

ALTER TABLE temporary_accommodation_premises ALTER COLUMN pdu SET NOT NULL;

-- Some TA premises do not have existing rows on the table, and will need them.
INSERT INTO temporary_accommodation_premises(premises_id, pdu)
SELECT id, 'Not specified'
FROM premises
WHERE premises.service = 'temporary-accommodation'
AND premises.id NOT IN (
    SELECT premises_id
    FROM temporary_accommodation_premises
);
