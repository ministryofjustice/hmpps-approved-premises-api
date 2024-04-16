ALTER TABLE users
ADD COLUMN probation_delivery_unit_id UUID;
ALTER TABLE users
ADD FOREIGN KEY (probation_delivery_unit_id) REFERENCES probation_delivery_units(id);