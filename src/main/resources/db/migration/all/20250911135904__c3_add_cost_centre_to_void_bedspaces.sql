
ALTER TABLE cas3_void_bedspaces ADD COLUMN cost_centre TEXT
    CONSTRAINT ck_cost_centre_type  CHECK (cost_centre = 'SUPPLIER' OR cost_centre = 'HMPPS');
