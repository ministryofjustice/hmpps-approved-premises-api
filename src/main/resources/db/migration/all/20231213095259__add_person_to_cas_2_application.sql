ALTER TABLE cas_2_applications ADD COLUMN pnc_number TEXT;
ALTER TABLE cas_2_applications ADD COLUMN name TEXT NOT NULL;
ALTER TABLE cas_2_applications ADD COLUMN date_of_birth DATE NOT NULL;
ALTER TABLE cas_2_applications ADD COLUMN nationality TEXT;
ALTER TABLE cas_2_applications ADD COLUMN sex TEXT;
ALTER TABLE cas_2_applications ADD COLUMN prison_name TEXT;
ALTER TABLE cas_2_applications ADD COLUMN person_status TEXT NOT NULL;