ALTER TABLE temporary_accommodation_applications
    ADD COLUMN probation_delivery_unit_id UUID;

ALTER TABLE temporary_accommodation_applications
    ADD FOREIGN KEY (probation_delivery_unit_id) REFERENCES probation_delivery_units(id);