-- Migrate premises characteristics to the real ones.
-- All mock characteristics defined correspond to premises-specific or shared real characteristics,
-- so no loss of data.

-- Migrate 'Park nearby' characteristic
UPDATE premises_characteristics
SET characteristic_id = 'fffb3004-5f0a-4e88-8350-fb89a0168296'
WHERE characteristic_id = '952790c0-21d7-4fd6-a7e1-9018f08d8bb0';

-- Migrate 'School nearby' characteristic
UPDATE premises_characteristics
SET characteristic_id = '78c5d99b-9702-4bf2-a23d-c2a0cf3a017d'
WHERE characteristic_id = '1dac11c5-a1b4-4313-b0f7-41b2e57f2404';

-- Migrate 'Not suitable for arson offenders' characteristic
UPDATE premises_characteristics
SET characteristic_id = '62c4d8cf-b612-4110-9e27-5c29982f9fcf'
WHERE characteristic_id = '16b52729-be75-43b4-b711-77fe5cbcb477';

-- Migrate 'Women only' characteristic
UPDATE premises_characteristics
SET characteristic_id = '8221f1ad-3aaf-406a-b918-dbdef956ea17'
WHERE characteristic_id = '7cd8dd2c-a14d-4a78-8d72-48449cc5aaa3';

-- Migrate 'Floor level access' characteristic
UPDATE premises_characteristics
SET characteristic_id = '99bb0f33-ff92-4606-9d1c-43bcf0c42ef4'
WHERE characteristic_id = 'd9f52ae8-4c01-4751-8975-992efacd5260';

-- Migrate room characteristics where possible.
-- Otherwise, delete the link entry.
-- This is fine, as this is a non-production Flyway migration.

-- 'Park nearby', 'School nearby', and 'Women only' characteristics are premises-specific, and have
-- no corresponding room characteristic to migrate to.
DELETE FROM room_characteristics
WHERE characteristic_id IN (
    '952790c0-21d7-4fd6-a7e1-9018f08d8bb0',
    '1dac11c5-a1b4-4313-b0f7-41b2e57f2404',
    '7cd8dd2c-a14d-4a78-8d72-48449cc5aaa3'
);

-- Migrate 'Not suitable for arson offenders' characteristic
UPDATE room_characteristics
SET characteristic_id = '62c4d8cf-b612-4110-9e27-5c29982f9fcf'
WHERE characteristic_id = '16b52729-be75-43b4-b711-77fe5cbcb477';

-- Migrate 'Floor level access' characteristic
UPDATE room_characteristics
SET characteristic_id = '99bb0f33-ff92-4606-9d1c-43bcf0c42ef4'
WHERE characteristic_id = 'd9f52ae8-4c01-4751-8975-992efacd5260';

-- Remove mock TA characteristics.
DELETE FROM characteristics
WHERE id IN (
    '952790c0-21d7-4fd6-a7e1-9018f08d8bb0',
    '1dac11c5-a1b4-4313-b0f7-41b2e57f2404',
    '16b52729-be75-43b4-b711-77fe5cbcb477',
    '7cd8dd2c-a14d-4a78-8d72-48449cc5aaa3',
    'd9f52ae8-4c01-4751-8975-992efacd5260'
);
