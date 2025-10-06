ALTER TABLE cas_2_users ADD COLUMN service_origin TEXT NOT NULL DEFAULT 'HDC';
ALTER TABLE cas_2_assessments ADD COLUMN service_origin TEXT NOT NULL DEFAULT 'HDC';
ALTER TABLE cas_2_applications ADD COLUMN service_origin TEXT NOT NULL DEFAULT 'HDC';
