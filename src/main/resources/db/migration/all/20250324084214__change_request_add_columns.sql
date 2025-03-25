ALTER TABLE cas1_change_requests ADD "version" int8 NOT NULL DEFAULT 1;
ALTER TABLE cas1_change_requests ADD decision_made_at timestamp with time zone NULL;
