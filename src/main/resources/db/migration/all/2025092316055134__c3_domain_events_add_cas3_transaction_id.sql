ALTER TABLE domain_events
    ADD COLUMN IF NOT EXISTS cas3_transaction_id UUID NULL;