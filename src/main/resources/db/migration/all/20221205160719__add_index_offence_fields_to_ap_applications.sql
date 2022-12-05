ALTER TABLE approved_premises_applications ADD COLUMN conviction_id BIGINT;
ALTER TABLE approved_premises_applications ADD COLUMN event_number TEXT;
ALTER TABLE approved_premises_applications ADD COLUMN offence_id TEXT;

UPDATE approved_premises_applications SET conviction_id = 0, event_number = '-1', offence_id = '-1';

ALTER TABLE approved_premises_applications ALTER COLUMN conviction_id SET NOT NULL;
ALTER TABLE approved_premises_applications ALTER COLUMN event_number SET NOT NULL;
ALTER TABLE approved_premises_applications ALTER COLUMN offence_id SET NOT NULL;
