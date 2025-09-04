ALTER TABLE cas_2_users ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE cas_2_users ADD COLUMN external_type TEXT;
ALTER TABLE cas_2_users ADD COLUMN nomis_account_type TEXT;