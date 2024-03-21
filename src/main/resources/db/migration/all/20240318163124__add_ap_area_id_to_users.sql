ALTER TABLE users
    ADD ap_area_id UUID NULL,
    ADD CONSTRAINT users_ap_area_id FOREIGN KEY (ap_area_id) REFERENCES ap_areas(id);