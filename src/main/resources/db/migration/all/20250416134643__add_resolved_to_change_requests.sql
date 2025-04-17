ALTER TABLE cas1_change_requests ADD resolved boolean NOT NULL default false;
ALTER TABLE cas1_change_requests ADD resolved_at timestamptz NULL;
ALTER TABLE cas1_change_requests DROP COLUMN decision_made_at;
