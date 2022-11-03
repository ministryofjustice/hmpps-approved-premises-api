ALTER TABLE premises ADD CONSTRAINT name_service_unique UNIQUE (name, service);
