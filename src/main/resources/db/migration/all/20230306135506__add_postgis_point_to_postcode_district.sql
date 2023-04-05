ALTER TABLE postcode_districts ADD COLUMN point geometry(point, 4326);
ALTER TABLE postcode_districts ALTER COLUMN "point" SET NOT NULL;
