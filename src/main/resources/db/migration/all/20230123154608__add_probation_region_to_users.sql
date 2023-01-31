ALTER TABLE "users" ADD COLUMN probation_region_id UUID;
ALTER TABLE "users" ADD FOREIGN KEY (probation_region_id) REFERENCES probation_regions(id);

-- Give existing users a default region.
UPDATE "users"
SET probation_region_id = (
    SELECT id
    FROM probation_regions
    WHERE delius_code = 'N51' -- North West
    LIMIT 1
);

ALTER TABLE "users" ALTER COLUMN probation_region_id SET NOT NULL;
