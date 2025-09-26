CREATE INDEX IF NOT EXISTS idx_premises_address_line1_lower ON premises(lower(address_line1));
CREATE INDEX IF NOT EXISTS idx_premises_address_line2_lower ON premises(lower(address_line2));
CREATE INDEX IF NOT EXISTS idx_premises_town_lower ON premises(lower(town));
CREATE INDEX IF NOT EXISTS idx_premises_postcode_lower ON premises(lower(postcode));
CREATE INDEX IF NOT EXISTS idx_premises_postcode_no_space_lower ON premises(lower(replace(postcode, ' ', '')));