ALTER TABLE move_on_categories ADD COLUMN service_scope TEXT;

UPDATE move_on_categories SET service_scope = 'approved-premises';

UPDATE move_on_categories
SET service_scope = '*'
WHERE id IN (
    '0d8dee49-f49a-4e0e-a20e-80e0c18645ce' -- Supported Housing
);

ALTER TABLE move_on_categories ALTER COLUMN service_scope SET NOT NULL;

INSERT INTO move_on_categories (id, name, is_active, service_scope) VALUES
    ('d8e04fa1-9757-4681-bfab-6c61913c8463', 'Approved Premises', true, 'temporary-accommodation'),
    ('247ae3f4-7958-4c59-97ed-272a39ad411c', 'Friends/Family (settled)', true, 'temporary-accommodation'),
    ('2236cd7e-32c5-4784-a461-8f0aead1d386', 'Householder (Owner - freehold or leasehold)', true, 'temporary-accommodation'),
    ('d3b9f02e-c3a5-475b-a5dd-124c058900d9', 'Long Term Residential Healthcare', true, 'temporary-accommodation'),
    ('587dc0dc-9073-4992-9d58-5576753050e9', 'Rental accommodation - private rental', true, 'temporary-accommodation'),
    ('12c739fa-7bb1-416d-bbf2-71362578a7f3', 'Rental accommodation - social rental', true, 'temporary-accommodation'),
    ('3de4665a-c848-4797-ba80-502cacc6f7d7', 'Friends/Family (transient)', true, 'temporary-accommodation'),
    ('a90a77a3-5662-4fa8-85ab-07d0c085052f', 'Homeless - Shelter/Emergency Hostel/Campsite', true, 'temporary-accommodation'),
    ('33244330-87e9-4cc6-9940-f78586585436', 'Homeless - Rough Sleeping', true, 'temporary-accommodation'),
    ('15fb0af2-3406-49c9-81ed-5e42bddf9fc2', 'Homeless - Squat', true, 'temporary-accommodation'),
    ('91158ed5-467e-4ee8-90d9-4f17a5dac82f', 'Transient/short term accommodation', true, 'temporary-accommodation'),
    ('9e18dc4d-297b-4a9c-86cc-31238d339b3a', 'AfEO Funded', true, 'temporary-accommodation');
