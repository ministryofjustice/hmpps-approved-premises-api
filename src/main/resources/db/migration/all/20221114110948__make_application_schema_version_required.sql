UPDATE applications SET schema_version = 'f96725f6-27ac-46f2-83e0-00cf4af48370';
ALTER TABLE applications ALTER COLUMN schema_version SET NOT NULL;
