ALTER TABLE applications ADD COLUMN version BIGINT NOT NULL DEFAULT 1;
ALTER TABLE placement_requests ADD COLUMN version BIGINT NOT NULL DEFAULT 1;
ALTER TABLE placement_applications ADD COLUMN version BIGINT NOT NULL DEFAULT 1;
ALTER TABLE assessments ADD COLUMN version BIGINT NOT NULL DEFAULT 1;
ALTER TABLE appeals ADD COLUMN version BIGINT NOT NULL DEFAULT 1;
