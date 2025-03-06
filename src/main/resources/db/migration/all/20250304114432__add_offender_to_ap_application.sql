CREATE EXTENSION IF NOT EXISTS pg_trgm;
ALTER TABLE cas1_offenders ADD COLUMN version BIGINT NOT NULL DEFAULT 1;
CREATE INDEX idx_cas1_offenders_crn ON cas1_offenders(crn);
CREATE INDEX idx_cas1_offenders_noms_number ON cas1_offenders(noms_number);
CREATE INDEX idx_name_trgm ON cas1_offenders USING gin (name gin_trgm_ops);


ALTER TABLE approved_premises_applications ADD cas1_offender_id uuid NULL;
ALTER TABLE approved_premises_applications ADD CONSTRAINT cas1_offender_fk FOREIGN KEY (cas1_offender_id) REFERENCES cas1_offenders(id);
