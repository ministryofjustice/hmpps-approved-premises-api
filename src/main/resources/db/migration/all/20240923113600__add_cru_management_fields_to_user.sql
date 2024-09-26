ALTER TABLE users ADD cas1_cru_management_area_id uuid NULL;
ALTER TABLE users ADD CONSTRAINT users_cas1_cru_management_area_id_fk FOREIGN KEY (cas1_cru_management_area_id) REFERENCES cas1_cru_management_areas(id);

ALTER TABLE users ADD cas1_cru_management_area_override_id uuid NULL;
ALTER TABLE users ADD CONSTRAINT users_cas1_cru_management_area_override_id_fk FOREIGN KEY (cas1_cru_management_area_override_id) REFERENCES cas1_cru_management_areas(id);
