CREATE INDEX idx_cases_tier_v2_tier_score ON cases (((tier_v2->>'tierScore')::text));
CREATE INDEX idx_cases_tier_v3_tier_score ON cases (((tier_v3->>'tierScore')::text));