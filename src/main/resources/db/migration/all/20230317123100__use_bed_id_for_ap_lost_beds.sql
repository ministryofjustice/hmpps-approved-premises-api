DELETE FROM lost_beds WHERE id NOT IN (SELECT id FROM temporary_accommodation_lost_beds);

ALTER TABLE lost_beds ADD COLUMN bed_id UUID CONSTRAINT bed_id_fk REFERENCES beds(id);

UPDATE lost_beds lb SET bed_id = (SELECT bed_id FROM temporary_accommodation_lost_beds talb WHERE lb.id = talb.lost_bed_id);

ALTER TABLE lost_beds ALTER COLUMN bed_id SET NOT NULL;

ALTER TABLE temporary_accommodation_lost_beds DROP COLUMN bed_id;

DROP TABLE approved_premises_lost_beds;
DROP TABLE temporary_accommodation_lost_beds;

ALTER TABLE lost_beds DROP COLUMN service;
