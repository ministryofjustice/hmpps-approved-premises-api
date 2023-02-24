ALTER TABLE lost_bed_reasons ADD COLUMN service_scope TEXT;

UPDATE lost_bed_reasons
SET service_scope = 'approved-premises';

ALTER TABLE lost_bed_reasons ALTER COLUMN service_scope SET NOT NULL;
