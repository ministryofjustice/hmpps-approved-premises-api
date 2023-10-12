ALTER TABLE domain_events ALTER COLUMN application_id DROP NOT NULL;

ALTER TABLE domain_events ADD COLUMN booking_id UUID;
ALTER TABLE domain_events ADD COLUMN service TEXT;

UPDATE domain_events
SET service = 'CAS1';

ALTER TABLE domain_events ALTER COLUMN service SET NOT NULL;
