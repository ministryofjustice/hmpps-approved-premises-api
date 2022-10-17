ALTER TABLE application_schemas RENAME TO json_schemas;
ALTER TABLE json_schemas ADD COLUMN type TEXT NOT NULL DEFAULT 'APPLICATION';
