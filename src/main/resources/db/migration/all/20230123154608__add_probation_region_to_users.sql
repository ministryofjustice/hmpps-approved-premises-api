ALTER TABLE "users" ADD COLUMN probation_region_id UUID NOT NULL;
ALTER TABLE "users" ADD FOREIGN KEY (probation_region_id) REFERENCES probation_regions(id);
