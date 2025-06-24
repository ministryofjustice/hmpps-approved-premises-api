ALTER TABLE domain_events
    ADD COLUMN IF NOT EXISTS cas3_premises_id UUID;
ALTER TABLE domain_events
    ADD COLUMN IF NOT EXISTS cas3_bedspace_id UUID;