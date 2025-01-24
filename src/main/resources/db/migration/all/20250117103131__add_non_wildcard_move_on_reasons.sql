-- add service_scope to the unique constraint
ALTER TABLE move_on_categories DROP CONSTRAINT move_on_category_name_key;
ALTER TABLE move_on_categories ADD CONSTRAINT move_on_category_name UNIQUE ("name",service_scope);

-- convert wildcard 'Supported Housing' to be CAS3 specific
UPDATE move_on_categories SET service_scope='temporary-accommodation' WHERE id='0d8dee49-f49a-4e0e-a20e-80e0c18645ce'::uuid;

-- add cas1 specific 'Supported Housing' category
INSERT INTO move_on_categories (id,"name",is_active,service_scope,legacy_delius_category_code)
VALUES ('6d65779a-9b99-451b-bd09-34aa9354c8cf','Supported Housing',true,'approved-premises','MC06');
