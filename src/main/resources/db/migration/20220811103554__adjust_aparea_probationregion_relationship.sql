ALTER TABLE probation_regions ADD COLUMN ap_area_id UUID NOT NULL;
ALTER TABLE probation_regions ADD CONSTRAINT ap_area_fk FOREIGN KEY (ap_area_id) REFERENCES ap_areas (id);
ALTER TABLE premises DROP COLUMN ap_area_id;

ALTER TABLE ap_areas ADD COLUMN identifier TEXT NOT NULL;
ALTER TABLE ap_areas ADD CONSTRAINT identifier_unique UNIQUE (identifier);
ALTER TABLE probation_regions DROP COLUMN identifier;
