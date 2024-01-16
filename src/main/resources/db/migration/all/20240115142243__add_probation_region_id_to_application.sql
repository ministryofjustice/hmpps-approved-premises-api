ALTER TABLE approved_premises_applications
    ADD COLUMN probation_region_id uuid NULL;

ALTER TABLE approved_premises_applications
    ADD CONSTRAINT approved_premises_applications_probation_region_id_fkey FOREIGN KEY (probation_region_id) REFERENCES probation_regions (id) DEFERRABLE;
