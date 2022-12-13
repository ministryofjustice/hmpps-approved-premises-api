ALTER TABLE cancellation_reasons ADD COLUMN service_scope TEXT;

UPDATE cancellation_reasons
SET service_scope = 'approved-premises';

ALTER TABLE cancellation_reasons ALTER COLUMN service_scope SET NOT NULL;
