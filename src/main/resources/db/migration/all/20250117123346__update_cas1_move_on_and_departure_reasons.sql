-- # Non Arrival

-- previously 'Other - provide reasons'
UPDATE non_arrival_reasons SET "name"='Other' WHERE id='3635c76e-8e4b-4c0e-8b92-149dc1ff0855'::uuid;

-- # Departure Reasons

-- CAS1 Left of own volition
UPDATE departure_reasons SET is_active=false WHERE id='2700152b-b904-449e-a6fd-50ae24d6fc7e'::uuid and service_scope = 'approved-premises';

-- # Move on Categories

-- CAS1 Not Applicable
UPDATE move_on_categories SET is_active=false WHERE id='ea3d79b0-1ee5-47ff-a7ae-b0d964ca7626'::uuid and service_scope = 'approved-premises';
-- CAS1 Local Authority - Rented
UPDATE move_on_categories SET is_active=false WHERE id='5387c2ed-b57f-4c31-8fd9-fee30ef32197'::uuid and service_scope = 'approved-premises';
-- CAS1 Housing Association - Rented
UPDATE move_on_categories SET is_active=false WHERE id='55d9f90f-454a-410e-b02d-4dc8ba536ac2'::uuid and service_scope = 'approved-premises';

INSERT INTO move_on_categories (id,"name",is_active,service_scope,legacy_delius_category_code)
VALUES ('2179f459-999c-472a-ab95-aa9a3fd72568','CAS3',true,'approved-premises','TBD');
INSERT INTO move_on_categories (id,"name",is_active,service_scope,legacy_delius_category_code)
VALUES ('6b041e91-6a76-4527-8832-999b413867b7','CAS2',true,'approved-premises','TBD');
INSERT INTO move_on_categories (id,"name",is_active,service_scope,legacy_delius_category_code)
VALUES ('cc938c03-ff64-4d31-9994-54eee8a17365','Local Authority / Housing Association - Rented',true,'approved-premises','TBD');
