ALTER TABLE probation_regions
    ADD CONSTRAINT probation_regions_ap_area_id_fk
        FOREIGN KEY (ap_area_id) REFERENCES ap_areas(id);
