ALTER TABLE approved_premises_applications
    ADD COLUMN ap_area_id uuid NULL;

ALTER TABLE approved_premises_applications
    ADD CONSTRAINT approved_premises_applications_ap_area_id_fkey FOREIGN KEY (ap_area_id) REFERENCES ap_areas (id) DEFERRABLE;
