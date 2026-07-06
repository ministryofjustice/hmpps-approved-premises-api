ALTER TABLE cases DROP COLUMN tier;

ALTER TABLE cases
ADD COLUMN tier_v2 jsonb,
ADD COLUMN tier_v3 jsonb;

