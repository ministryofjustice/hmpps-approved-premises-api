ALTER TABLE temporary_accommodation_applications ADD COLUMN conviction_id BIGINT NOT NULL;
ALTER TABLE temporary_accommodation_applications ADD COLUMN event_number TEXT NOT NULL;
ALTER TABLE temporary_accommodation_applications ADD COLUMN offence_id TEXT NOT NULL;
ALTER TABLE temporary_accommodation_applications ADD COLUMN probation_region_id UUID NOT NULL;
ALTER TABLE temporary_accommodation_applications ADD COLUMN risk_ratings JSON;

ALTER TABLE temporary_accommodation_applications ADD FOREIGN KEY (probation_region_id) REFERENCES probation_regions(id);
