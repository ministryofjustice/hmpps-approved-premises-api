ALTER TABLE lost_beds DROP COLUMN reason;
ALTER TABLE lost_beds ADD COLUMN lost_bed_reason_id UUID NOT NULL;
ALTER TABLE lost_beds ADD CONSTRAINT lost_bed_reason_id_fk FOREIGN KEY (lost_bed_reason_id) REFERENCES lost_bed_reasons(id);
