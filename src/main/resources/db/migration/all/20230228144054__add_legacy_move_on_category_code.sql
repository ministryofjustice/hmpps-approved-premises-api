ALTER TABLE move_on_categories ADD COLUMN legacy_delius_category_code TEXT;

UPDATE move_on_categories SET legacy_delius_category_code = 'MC05' WHERE id = '44bb3d42-c162-455a-b16b-d998961f98fd';
UPDATE move_on_categories SET legacy_delius_category_code = 'MC19' WHERE id = '48ad4a94-f81f-4cd5-a564-ad1974d5cf67';
UPDATE move_on_categories SET legacy_delius_category_code = 'MC10' WHERE id = 'e0dacde6-ddcf-4c20-af58-8fd858eb4171';
UPDATE move_on_categories SET legacy_delius_category_code = 'MC03' WHERE id = '55d9f90f-454a-410e-b02d-4dc8ba536ac2';
UPDATE move_on_categories SET legacy_delius_category_code = 'MC07' WHERE id = 'e11e59e4-75fe-462c-9850-80764b95acc7';
UPDATE move_on_categories SET legacy_delius_category_code = 'MC02' WHERE id = '5387c2ed-b57f-4c31-8fd9-fee30ef32197';
UPDATE move_on_categories SET legacy_delius_category_code = 'MC14' WHERE id = 'e1436d4b-5723-4e92-b53c-2b6c6babe0b3';
UPDATE move_on_categories SET legacy_delius_category_code = 'MCNA' WHERE id = 'ea3d79b0-1ee5-47ff-a7ae-b0d964ca7626';
UPDATE move_on_categories SET legacy_delius_category_code = 'MC08' WHERE id = 'ff40dd76-c9eb-48a5-9ac8-9f51f03dbf48';
UPDATE move_on_categories SET legacy_delius_category_code = 'MC04' WHERE id = '9ed75736-83bb-4387-9eec-19a6e6343357';
UPDATE move_on_categories SET legacy_delius_category_code = 'MC06' WHERE id = '0d8dee49-f49a-4e0e-a20e-80e0c18645ce';
UPDATE move_on_categories SET legacy_delius_category_code = 'P' WHERE id = '6b1f6645-dc1c-489d-8312-cab9a4a6b2a7';
