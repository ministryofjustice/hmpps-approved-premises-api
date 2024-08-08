CREATE EXTENSION IF NOT EXISTS  pg_trgm;
CREATE INDEX temporary_accommodation_applications_lower_name_trgm_idx ON temporary_accommodation_applications USING gin (lower(name) gin_trgm_ops);
